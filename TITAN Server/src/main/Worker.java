package main;

import io.ConnectionInfo;
import io.NioUnit;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import objects.Message;
import objects.PacketType;
import objects.RequestType;
import objects.User;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.javatuples.Pair;

import components.DBConnector;
import components.DataUtils;
import components.Encryption;
import components.PacketBuilder;

import events.ClientRegisterEvent;
import events.ClientRemoveEvent;
import events.DataReceivedEvent;
import events.Event;
import events.EventHandler;
import events.LogEvent;
import frame.LogFrame;

public class Worker implements Runnable, EventHandler {

	public Worker() {
		eventQueue = new LinkedBlockingQueue<Event>();
	}

	@Override
	public void run() {
		init();
		loop();
	}

	public static LinkedBlockingQueue<Event>	eventQueue;
	public static BcPGPKeyPair					serverKeyPair;
	private static boolean						closed;
	private static byte[]						publicKeyBytes;
	private static ExecutorService				executorService;
	private static NioUnit						ioServer;

	private void init() {
		try {
			serverKeyPair = Encryption.genRsaKeyRing();
			publicKeyBytes = Encryption.keyToBytes(serverKeyPair.getPublicKey());
		} catch (PGPException | IOException e) {
			// TODO Auto-generated catch block
			LogFrame.updateLog(e);
			System.exit(-1);
		}

		executorService = Executors.newCachedThreadPool();
		ioServer = MainServer.getIoServer();
	}

	private void loop() {
		while (!closed) {
			try {
				handleNewEvents();
				checkAndSendUnreadMessages();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	private void handleNewEvents() {
		Event event = null;
		try {
			event = eventQueue.poll(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			LogFrame.updateLog(e);
		}
		if (event != null) {
			executorService.submit(new EventExecutor(event));
		}
	}

	@Override
	public void handleEvent(Event event) {
		try {
			eventQueue.put(event);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			LogFrame.updateLog(e);
		}
	}

	private void checkAndSendUnreadMessages() {
		if (ClientManagement.getClientCount() > 0) {
			Set<Integer> clientSet = ClientManagement.getAllIDs();
			for (Integer id : clientSet) {
				if (ClientManagement.getChannel(id) == null ||
						ioServer.getConnectionInfo(ClientManagement.getChannel(id)) == null ||
						!ioServer.getConnectionInfo(ClientManagement.getChannel(id)).isReadyToReceiveMessages()) {
					continue;
				}
				Message[] messagesToSend = null;
				try {
					messagesToSend = DBConnector.checkUnreadMessages(id);
				} catch (SQLException | ParseException e) {
					// TODO Auto-generated catch block
					LogFrame.updateLog(e);
					continue;
				}
				if (messagesToSend == null) {
					continue;
				}
				SocketChannel socketChannel = ClientManagement.getChannel(id);
				for (Message message : messagesToSend) {
					try {
						ioServer.send(socketChannel, PacketBuilder.serialize(ioServer.getConnectionInfo(socketChannel)
								.getKey(),
							PacketType.MessagePacket,
							DataUtils.intToByteArray(message.getSender()),
							DataUtils.intToByteArray(message.getID()),
							message.getContent().getBytes(Charset.forName("UTF-8")),
							DataUtils.longToBytes(message.getTime().getTime())));
					} catch (IOException | PGPException e) {
						// TODO Auto-generated catch block
						LogFrame.updateLog(e);
					}
				}
			}
		} else {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block

			}
		}
	}

	private class EventExecutor implements Runnable {

		private Event	event;

		public EventExecutor(Event event) {
			this.event = event;
		}

		@Override
		public void run() {
			try {
				System.out.println("Handling Event: " + event.toString());
				Thread.currentThread().setName("EventExecutor");
				if (event instanceof DataReceivedEvent) {
					DataReceivedEvent dataReceivedEvent = (DataReceivedEvent) event;
					Pair<Integer, byte[][]> packet = Pair.with(null, null);
					packet = PacketBuilder.unseriealize(dataReceivedEvent.getBytes());
					System.out.println("PacketType: " + packet.getValue0());
					if (packet.getValue1() != null) {
						ConnectionInfo connectionInfo = ioServer
								.getConnectionInfo(dataReceivedEvent.getSocketChannel());
						int type = packet.getValue0();
						byte[][] received = packet.getValue1();
						switch (type) {
							case PacketType.KeyPacket:
								System.out.println("KeyPacket received!");
								ioServer.getConnectionInfo(dataReceivedEvent.getSocketChannel())
										.setKey(Encryption.bytesToPublicKey(received[0]));
								ioServer.send(dataReceivedEvent.getSocketChannel(), PacketBuilder
										.serialize(connectionInfo.getKey(),
											PacketType.StatusPacket,
											PacketBuilder.SuccessPacket));
								break;
							case PacketType.LoginPacket:
								String userName = String.valueOf(DataUtils.bytesToChar(received[0]));
								char[] password = DataUtils.bytesToChar(received[1]);
								LogFrame.updateLog("ID: " + connectionInfo.getId() + " logged in as " + userName +
										" with password: " + String.valueOf(password));
								byte[] toSend = null;
								User loggedin;
								byte[] loginStatus;
								//TODO: Rewrite Login
								if ((loggedin = DBConnector.check(userName, password)) != null) {
									LogFrame.updateLog("Login succeded!");
									loginStatus = PacketBuilder.SuccessPacket;

									toSend = PacketBuilder.serialize(connectionInfo.getKey(),
										PacketType.StatusPacket,
										DataUtils.intToByteArray(PacketType.LoginPacket),
										loginStatus);
									ioServer.send(dataReceivedEvent.getSocketChannel(), toSend);

									ClientManagement.changeID(ClientManagement.getID(dataReceivedEvent
											.getSocketChannel()), loggedin.getID());
									connectionInfo.setId(loggedin.getID());
									connectionInfo.setLoggedIn(true);
									ioServer.setConnectionInfo(dataReceivedEvent.getSocketChannel(), connectionInfo);
									LogFrame.updateLog("User: " + loggedin.getUsername() + " has now ID: " +
											loggedin.getID());
								} else {
									LogFrame.updateLog("Login failed!");
									loginStatus = PacketBuilder.FailPacket;

									toSend = PacketBuilder.serialize(connectionInfo.getKey(),
										PacketType.StatusPacket,
										DataUtils.intToByteArray(PacketType.LoginPacket),
										loginStatus);
									ioServer.send(dataReceivedEvent.getSocketChannel(), toSend);

								}
								break;
							case PacketType.MessagePacket:
								int receiverID = DataUtils.byteArrayToInt(received[0]);
								int senderID = connectionInfo.getId();
								String message = new String(received[1], Charset.forName("UTF-8"));
								long time = DataUtils.bytesToLong(received[2]);
								LogFrame.updateLog(senderID + " -> " + receiverID + " : " + message);
								int messageId = 0;

								messageId = DBConnector.putMessage(senderID, receiverID, message, time);
								if (ClientManagement.isOnline(receiverID)) {
									ioServer.send(dataReceivedEvent.getSocketChannel(), PacketBuilder
											.serialize(connectionInfo.getKey(),
												PacketType.MessagePacket,
												DataUtils.intToByteArray(senderID),
												DataUtils.intToByteArray(messageId),
												received[1],
												DataUtils.longToBytes(time)));
								}
								break;
							case PacketType.StatusPacket:
								int packetType = DataUtils.byteArrayToInt(received[0]);
								String status = new String();
								if (received[1] == PacketBuilder.SuccessPacket) {
									status = "success";
								} else {
									status = "failed";
								}

								switch (packetType) {
									case PacketType.LoginPacket:
										ioServer.getConnectionInfo(dataReceivedEvent.getSocketChannel())
												.setReadyToReceiveMessages(true);
										LogFrame.updateLog("Client succeded logging in.");
										break;
									default:
										LogFrame.updateLog("Received StatusPacket for " +
												PacketType.getType(packetType) + " Status: " + status);
										break;
								}
								break;

							case PacketType.RequestPacket:
								int requestType = DataUtils.byteArrayToInt(received[0]);
								switch (requestType) {
									case RequestType.KeyRequest:

										byte[] dataToSend = PacketBuilder.serialize(PacketType.KeyPacket,
											publicKeyBytes);
										ioServer.send(dataReceivedEvent.getSocketChannel(), dataToSend);

										break;
									// @deprecated
									//
									// case RequestType.ContactList:
									// User[] users = null;
									//
									// users =
									// DBConnector.getFriendsOf(connectionInfo.getId()());
									// byte[][] list = new byte[users.length][];
									// int counter = 0;
									// for (User user : users) {
									// list[counter] =
									// DataUtils.ObjectToByteArray(user);
									// counter++;
									// }
									//
									// senderQueue.offer(new
									// Data(connectionInfo.getId()(),
									// PacketBuilder.serialize(
									// PacketType.ContactListPacket, list)));
									// break;
									case RequestType.User:
										if (!connectionInfo.isLoggedIn()) {
											ioServer.send(dataReceivedEvent.getSocketChannel(),
												PacketBuilder.serialize(PacketType.StatusPacket,
													DataUtils.intToByteArray(PacketType.UserPacket),
													PacketBuilder.FailPacket));
											break;
										}
										User user = DBConnector.get(new String(received[1], Charset.forName("UTF-8")));
										ioServer.send(dataReceivedEvent.getSocketChannel(), PacketBuilder
												.serialize(connectionInfo.getKey(),
													PacketType.UserPacket,
													DataUtils.ObjectToByteArray(user)));

										break;
									case RequestType.FriendRequest:
										if (!connectionInfo.isLoggedIn()) {
											ioServer.send(dataReceivedEvent.getSocketChannel(),
												PacketBuilder.serialize(PacketType.StatusPacket,
													DataUtils.intToByteArray(PacketType.UserPacket),
													PacketBuilder.FailPacket));
											break;
										}
										User friend = DBConnector.setFriends(connectionInfo.getId(),
											new String(received[1], Charset.forName("UTF-8")));
										if (friend != null) {
											LogFrame.updateLog(connectionInfo.getId() + " and " + friend.getID() +
													" are now Friends!");
											ioServer.send(dataReceivedEvent.getSocketChannel(), PacketBuilder
													.serialize(connectionInfo.getKey(),
														PacketType.UserPacket,
														DataUtils.ObjectToByteArray(friend)));
										} else {
											ioServer.send(dataReceivedEvent.getSocketChannel(), PacketBuilder
													.serialize(connectionInfo.getKey(),
														PacketType.StatusPacket,
														PacketBuilder.FailPacket));
										}
										break;
									case RequestType.FriendRemoveRequest:
										break;
									default:
										LogFrame.updateLog("Invalid RequestPacket! Type: " + type);
										break;
								}
								break;
							default:
								StringBuilder sb = new StringBuilder();
								for (byte[] array : packet.getValue1()) {
									for (byte b : array) {
										sb.append(b);
										sb.append(' ');
									}
									sb.append(' ');
								}

								LogFrame.updateLog("Error! Received PacketType: " + packet.getValue0() + " form ID: " +
										connectionInfo.getId() + "\nPrinting out packet data: " + sb.toString());

								toSend = PacketBuilder.serialize(PacketType.StatusPacket,
									DataUtils.intToByteArray(packet.getValue0()),
									new byte[] { 0 });
								ioServer.send(dataReceivedEvent.getSocketChannel(), toSend);

								break;
						}
					}
				} else if (event instanceof ClientRegisterEvent) {
					ClientRegisterEvent clientRegisterEvent = (ClientRegisterEvent) event;
					ioServer.setConnectionInfo(clientRegisterEvent.getSocketChannel(),
						new ConnectionInfo(0, ConnectionInfo.LOCALHOSTCONECTION));
					ClientManagement.register(0, clientRegisterEvent.getSocketChannel());
					//TODO: Fix Temp IDS!
				} else if (event instanceof ClientRemoveEvent) {
					ClientRemoveEvent clientRemoveEvent = (ClientRemoveEvent) event;
					ClientManagement.remove(clientRemoveEvent.getSocketChannel());
				} else if (event instanceof LogEvent) {
					LogEvent logEvent = (LogEvent) event;
					LogFrame.updateLog(logEvent.getMessage());
				} else {
					LogFrame.updateLog("Unhandled Event: " + event.toString());
				}
			} catch (Throwable t) {
				LogFrame.updateLog(t);
			}

		}
	}

	public synchronized void close() {
		closed = true;
		executorService.shutdown();
	}

}