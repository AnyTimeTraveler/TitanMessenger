package components;

import io.NioUnit;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import javax.swing.JOptionPane;

import main.Config;
import objects.Message;
import objects.PacketType;
import objects.RequestType;
import objects.Server;
import objects.User;

import org.bouncycastle.openpgp.PGPException;
import org.javatuples.Pair;

import visual.MessengerFrame;
import actions.Action;
import actions.AddFriendAction;
import actions.LoginAction;
import actions.RemoveFriendAction;
import events.ActionEvent;
import events.DataReceivedEvent;
import events.Event;
import events.LogEvent;

public class DataHandler implements events.EventHandler {

	private static final int					TIMEOUT_IN_SEC		= 10;
	private static final long					CHECKRATE			= 250;

	private static LinkedBlockingQueue<byte[]>	receiveQueue;
	private static LinkedBlockingQueue<Message>	messengerFrameQueue;
	private static LinkedBlockingQueue<Action>	actionQueue;
	private static LinkedBlockingQueue<Event>	eventQueue;
	private static NioUnit						ioClient;
	private static SocketChannel				serverSocketChannel;
	private static boolean						closed;
	private static DataHandler					instance;
	private static ExecutorService				actionExecService;
	private static boolean						loginComplete		= false;
	private static boolean						loggedIn			= false;
	private boolean								waitingForPacket	= false;
	private static ExecutorService				eventExecService;

	public DataHandler(NioUnit ioUnit, SocketChannel serverSocketChannel) {
		DataHandler.ioClient = ioUnit;
		DataHandler.serverSocketChannel = serverSocketChannel;
		init();
	}

	public DataHandler() {
		init();
	}

	private void init() {
		DataHandler.actionExecService = Executors.newSingleThreadExecutor();
		DataHandler.eventExecService = Executors.newSingleThreadExecutor();
		actionQueue = new LinkedBlockingQueue<Action>();
		eventQueue = new LinkedBlockingQueue<Event>();
		messengerFrameQueue = new LinkedBlockingQueue<Message>();
		receiveQueue = new LinkedBlockingQueue<byte[]>();
	}

	public void setIOUnit(NioUnit ioUnit) {
		DataHandler.ioClient = ioUnit;
	}

	public void setServerChannel(SocketChannel serverSocketChannel) {
		DataHandler.serverSocketChannel = serverSocketChannel;
	}

	public DataHandler getInstance() {
		return instance;
	}

	public static LinkedBlockingQueue<Message> getMainFrameQueue() {
		return messengerFrameQueue;
	}

	@Override
	public void run() {
		Action currentAction = null;
		Event currentEvent = null;
		while (!closed) {

			try {
				currentAction = actionQueue.poll(CHECKRATE, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				currentEvent = eventQueue.poll(CHECKRATE, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (currentAction != null) {
				actionExecService.submit(new ActionExecutor(currentAction));
			} else if (currentEvent != null) {
				eventExecService.submit(new EventExecutor(currentEvent));
			}
		}
	}

	/**
	 * 
	 */
	public static void resetLogin() {
		loginComplete = false;
		loggedIn = false;
	}

	public static void login(Server server, String username, char[] password, Observer loginObserver) {
		resetLogin();
		actionQueue.add(new LoginAction(server, username, password, loginObserver));
	}

	public static void sendMessage(int receiverID, String message, Date time) throws IOException, PGPException {
		sendMessage(receiverID, message, time.getTime());
	}

	public static void sendMessage(int receiverID, String message, long time) throws IOException, PGPException {
		ioClient.send(serverSocketChannel,
			PacketBuilder.serialize(true,
				PacketType.MessagePacket,
				DataUtils.intToByteArray(receiverID),
				message.getBytes(Charset.forName("UTF-8")),
				DataUtils.longToBytes(time)));
	}

	public static void addFriend(String username) throws IOException, InterruptedException, DataFormatException, PGPException {
		actionQueue.add(new AddFriendAction(username));
	}

	public static void removeFriend(int id) {
		actionQueue.add(new RemoveFriendAction(id));
	}

	@Override
	public void handleEvent(Event event) {
		eventQueue.add(event);
	}

	public static boolean isLoginComplete() {
		return loginComplete;
	}

	public static boolean isLoggedIn() {
		return loggedIn;
	}

	private class ActionExecutor extends Observable implements Runnable {

		private Action	currentAction;

		public ActionExecutor(Action currentAction) {
			this.currentAction = currentAction;
		}

		private void updateLoginProgress(String string) {
			this.notifyObservers(string);
			this.setChanged();
			System.out.println(string);
		}

		@Override
		public void run() {
			Thread.currentThread().setName("ActionExecutor");
			if (currentAction instanceof LoginAction) {
				waitingForPacket = true;
				LoginAction la = (LoginAction) currentAction;

				this.addObserver(la.getObserver());

				DBConnector.connect(la.getUsername());

				// First, check for the ServerKey and get it, if needed.
				if (serverSocketChannel == null) {

					updateLoginProgress("Connecting...");

					try {
						serverSocketChannel = ioClient.connect(la.getServer().getAddress(),
							la.getServer().getPort(),
							10 * 1000);
					} catch (IOException | InterruptedException e) {
						updateLoginProgress("Connecting...Failed!");
						e.printStackTrace();
						handleEvent(new ActionEvent(Event.Login, e.getStackTrace()));
						return;
					}

					updateLoginProgress("Connecting...OK");
				}

				try {
					if (Config.getInstance().ServerKey == null) {

						updateLoginProgress("Downloading ServerKey...");

						ioClient.send(serverSocketChannel,
							PacketBuilder.serialize(false,
								PacketType.RequestPacket,
								DataUtils.intToByteArray(RequestType.KeyRequest)));
						MyCustomTimer timer = new MyCustomTimer();
						byte[] receivedBytes = receiveQueue.poll(TIMEOUT_IN_SEC, TimeUnit.SECONDS);
						timer.getTimePassed();
						if (receivedBytes == null) {
							throw new Exception("No answer from Server after sending KeyRequest");
						}

						Pair<Integer, byte[][]> keyPacket = PacketBuilder.unseriealize(receivedBytes);

						if (keyPacket.getValue0() != PacketType.KeyPacket) {
							throw new Exception("Error during deserialisazion of the ServerKey");
						}

						Config.getInstance().ServerKey = Encryption.bytesToPublicKey(keyPacket.getValue1()[0]);

						if (Config.getInstance().ServerKey == null) {
							throw new Exception("Unable to convert received bytes to ServerKey");
						}
					}

				} catch (Exception e) {
					updateLoginProgress("Downloading ServerKey...Failed!");
					e.printStackTrace();
					handleEvent(new ActionEvent(ActionEvent.Login, e.getStackTrace()));
					return;
				}

				updateLoginProgress("Downloading ServerKey...OK");

				// Send our own key

				updateLoginProgress("Uploading ClientKey...");

				try {
					ioClient.send(serverSocketChannel,
						PacketBuilder.serialize(true,
							PacketType.KeyPacket,
							Encryption.keyToBytes(Config.getInstance().ClientKeyPair.getPublicKey())));
					byte[] receivedBytes = receiveQueue.poll(TIMEOUT_IN_SEC, TimeUnit.SECONDS);

					if (receivedBytes == null) {
						throw new Exception("No answer from Server after sending ClientKey");
					}

					Pair<Integer, byte[][]> responsePacket = PacketBuilder.unseriealize(receivedBytes);

					if (responsePacket == null || responsePacket.getValue0() != PacketType.StatusPacket) {
						throw new Exception("Error during deserialisazion of the StatusPacket");
					}

					if (responsePacket.getValue1()[0][0] != (byte) 1) {
						throw new Exception("StatusPacket reports a problem.");
					}

				} catch (Exception e) {
					updateLoginProgress("Uploading ClientKey...Failed!");
					e.printStackTrace();
					handleEvent(new ActionEvent(ActionEvent.Login, e.getStackTrace()));
					return;
				}

				updateLoginProgress("Uploading ClientKey...OK");

				// Now Login

				updateLoginProgress("Logging in...");

				try {

					ioClient.send(serverSocketChannel, PacketBuilder.serialize(true, PacketType.LoginPacket, la
							.getUsername().getBytes("UTF-8"), DataUtils.charToBytes(la.getPassword())));
					byte[] receivedBytes = receiveQueue.poll(TIMEOUT_IN_SEC, TimeUnit.SECONDS);

					if (receivedBytes == null) {
						throw new Exception("No answer from the Server after sending LoginPacket");
					}

					Pair<Integer, byte[][]> response = PacketBuilder.unseriealize(receivedBytes);

					if (response == null || response.getValue1() == null) {
						throw new Exception("Error unserializing received response");
					}

					if (!(response.getValue0() == PacketType.StatusPacket && DataUtils.byteArrayToInt(response
							.getValue1()[0]) == PacketType.LoginPacket)) {
						throw new Exception("Invalid response");
					}
					byte[] answer = response.getValue1()[1];
					if (Arrays.equals(answer, PacketBuilder.FailPacket)) {
						throw new Exception("Wrong Username and/or Password");
					}

					ioClient.send(serverSocketChannel,
						PacketBuilder.serialize(true,
							PacketType.RequestPacket,
							DataUtils.intToByteArray(RequestType.User),
							la.getUsername().getBytes(StandardCharsets.UTF_8)));
					receivedBytes = null;
					receivedBytes = receiveQueue.poll(TIMEOUT_IN_SEC, TimeUnit.SECONDS);

					if (receivedBytes == null) {
						throw new Exception("No answer from the Server after sending UserRequestPacket");
					}
					response = null;
					response = PacketBuilder.unseriealize(receivedBytes);

					if (response == null || response.getValue1() == null) {
						throw new Exception("Error unserializing received response");
					}

					if (response.getValue0() == PacketType.StatusPacket) {
						// When it's a StatusPacket then the operation failed.
						throw new Exception("Failed to receive Userdata");
					} else if (response.getValue0() == PacketType.UserPacket) {
						Config.getInstance().thisUser = (User) DataUtils.byteArrayToObject(response.getValue1()[0]);
					} else {
						throw new Exception("Received wrong PacketType!\nReceived PacketType: " + response.getValue0() +
								"\nExpected PacketType: " + PacketType.StatusPacket);
					}

					handleEvent(new ActionEvent(ActionEvent.Login, true));
				} catch (Exception e) {
					updateLoginProgress("Logging in...Failed!");
					e.printStackTrace();
					handleEvent(new ActionEvent(ActionEvent.Login, e.getStackTrace()));
					return;
				}
				//TODO: Rework the following
				//Start verification-progress

				try {
					byte[] successPacket = PacketBuilder.serialize(true,
						PacketType.StatusPacket,
						DataUtils.intToByteArray(PacketType.LoginPacket),
						PacketBuilder.SuccessPacket);
					ioClient.send(serverSocketChannel, successPacket);
				} catch (IOException | PGPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//End 
				updateLoginProgress("Logging in...OK");
				handleEvent(new ActionEvent(ActionEvent.Login, true));
				return;
			} else if (currentAction instanceof AddFriendAction) {
				AddFriendAction addFriendAction = (AddFriendAction) currentAction;
				waitingForPacket = true;
				try {
					ioClient.send(serverSocketChannel,
						PacketBuilder.serialize(true,
							PacketType.RequestPacket,
							DataUtils.intToByteArray(RequestType.FriendRequest),
							addFriendAction.getUsername().getBytes(Charset.forName("UTF-8"))));
					byte[] receivedBytes = receiveQueue.poll(TIMEOUT_IN_SEC, TimeUnit.SECONDS);

					if (receivedBytes == null) {
						throw new Exception("No answer from the Server after sending RequestPacket");
					}

					Pair<Integer, byte[][]> response = PacketBuilder.unseriealize(receivedBytes);

					if (response.getValue0() == PacketType.UserPacket) {
						DBConnector.addToContacts((User) DataUtils.byteArrayToObject(response.getValue1()[0]));
						MessengerFrame.getInstance().updateContactList();
					} else if (response.getValue0() == PacketType.StatusPacket) {
						JOptionPane.showMessageDialog(MessengerFrame.getInstance(),
							"Could not find username",
							"Error",
							JOptionPane.ERROR_MESSAGE);
					} else {
						throw new Exception("Invalid Packeage");
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					waitingForPacket = false;
				}

			} else if (currentAction instanceof RemoveFriendAction) {
				RemoveFriendAction removeFriendAction = (RemoveFriendAction) currentAction;

				try {
					ioClient.send(serverSocketChannel,
						PacketBuilder.serialize(true,
							PacketType.RequestPacket,
							DataUtils.intToByteArray(RequestType.FriendRemoveRequest),
							DataUtils.intToByteArray(removeFriendAction.getId())));
					DBConnector.removeFromContacts(removeFriendAction.getId());
				} catch (IOException | PGPException | SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return;
		}
	}

	private class EventExecutor implements Runnable {

		private Event	currentEvent;

		public EventExecutor(Event currentEvent) {
			this.currentEvent = currentEvent;
		}

		@Override
		public void run() {
			Thread.currentThread().setName("EventExecutor");
			if (currentEvent instanceof LogEvent) {
				LogEvent logEvent = (LogEvent) currentEvent;
				System.out.println(logEvent.getMessage());
			} else if (currentEvent instanceof ActionEvent) {
				ActionEvent actionEvent = (ActionEvent) currentEvent;
				switch (actionEvent.getEventType()) {
					case Event.Login:
						if (actionEvent.wasSuccessful()) {
							loggedIn = true;
						} else {
							loggedIn = false;
						}
						loginComplete = true;
						waitingForPacket = false;
						break;
					default:
						System.out.println("Event: " + actionEvent.getEventType() + " : " +
								actionEvent.getEventString());
						break;
				}
			} else if (currentEvent instanceof DataReceivedEvent) {
				DataReceivedEvent dataReceivedEvent = (DataReceivedEvent) currentEvent;
				if (waitingForPacket) {
					try {
						receiveQueue.put(dataReceivedEvent.getData().getBytes());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					byte[] receivedData = dataReceivedEvent.getData().getBytes();

					Pair<Integer, byte[][]> receivedPacket;
					try {
						receivedPacket = PacketBuilder.unseriealize(receivedData);

						int packetType = receivedPacket.getValue0();
						byte[][] packetData = receivedPacket.getValue1();

						switch (packetType) {
							case PacketType.MessagePacket:

								int sender = DataUtils.byteArrayToInt(packetData[0]);
								int id = DataUtils.byteArrayToInt(packetData[1]);
								String message = new String(packetData[2], StandardCharsets.UTF_8);
								long time = DataUtils.bytesToLong(packetData[3]);
								messengerFrameQueue.offer(new Message(	sender,
																		Config.getInstance().thisUser.getID(),
																		message,
																		id,
																		new Date(time)));
								DBConnector.addToChatHistory(sender, Config.getInstance().thisUser.getID(), message);
								break;
							default:
								break;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void close() {
		actionExecService.shutdown();
		eventExecService.shutdown();
		closed = true;
	}
}
