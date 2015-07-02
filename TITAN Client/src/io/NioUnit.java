package io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import objects.Data;
import events.ClientRegisterEvent;
import events.ClientRemoveEvent;
import events.DataReceivedEvent;
import events.EventHandler;
import events.LogEvent;

public class NioUnit implements Runnable {
	// The host:port combination to listen on
	private InetAddress hostAddress;
	private int hostPort;

	// The channel on which we'll accept connections
	private ServerSocketChannel serverChannel;

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer;

	private EventHandler eventHandler;

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
	private boolean closed;
	private boolean ready;

	/**
	 * Initializes the NioUnit without opening a ServerSocketChannel, which
	 * limits the NioUnit to Client-Functionality.
	 * 
	 * @param eventHandler
	 * @throws IOException
	 */
	public NioUnit(EventHandler eventHandler) throws IOException {
		this.selector = this.initSelector();
		this.eventHandler = eventHandler;
		this.readBuffer = ByteBuffer.allocate(10240);
		closed = false;
	}

	/**
	 * Initializes the NioUnit and opens a ServerSocketChannel, which provides
	 * Server-Functionality.
	 * 
	 * @param hostAddress
	 * @param port
	 * @param eventHandler
	 * @throws IOException
	 */
	public NioUnit(InetAddress hostAddress, int port, EventHandler eventHandler) throws IOException {
		this.hostAddress = hostAddress;
		this.hostPort = port;
		this.eventHandler = eventHandler;
		this.selector = this.initSelector();
		createServerSocketChannel();
		this.readBuffer = ByteBuffer.allocate(10240);
		closed = false;
	}

	/**
	 * 
	 * @throws IOException
	 */
	public NioUnit() throws IOException {
		this.selector = this.initSelector();
		createServerSocketChannel();
		this.readBuffer = ByteBuffer.allocate(10240);
		closed = false;
	}

	/**
	 * Connects to a remote host and registers the resulting SocketChannel. This
	 * method is blocking until the connection is established or the timeout is
	 * reached.
	 * 
	 * @param Remote
	 *            host to connect to
	 * @param Remote
	 *            port
	 * @param Timeout
	 *            in Milliseconds
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public SocketChannel connect(InetAddress remoteHost, int remotePort, int timeout) throws IOException,
			InterruptedException {
		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		// Kick off connection establishment
		socketChannel.connect(new InetSocketAddress(remoteHost, remotePort));

		// Queue a channel registration since the caller is not the
		// selecting thread. As part of the registration we'll register
		// an interest in connection events. These are raised when a channel
		// is ready to complete connection establishment.
		synchronized (this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		}
		this.selector.wakeup();
		int timeoutCounter = 0;
		while ((!socketChannel.isConnected()) && timeoutCounter < timeout) {
			socketChannel.finishConnect();
			Thread.sleep(100);
			timeoutCounter += 100;
		}
		if (socketChannel.isConnected()) {
			return socketChannel;

		}
		socketChannel.close();
		return null;
	}

	/**
	 * 
	 * @param remoteHost
	 * @param remotePort
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public SocketChannel connect(InetAddress remoteHost, int remotePort) throws IOException, InterruptedException {
		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		// Kick off connection establishment
		socketChannel.connect(new InetSocketAddress(remoteHost, remotePort));

		// Queue a channel registration since the caller is not the
		// selecting thread. As part of the registration we'll register
		// an interest in connection events. These are raised when a channel
		// is ready to complete connection establishment.
		synchronized (this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		}
		this.selector.wakeup();
		return socketChannel;
	}

	public void disconnect(SocketChannel sc) throws IOException {
		sc.close();
		if (sc.keyFor(selector) != null) {
			sc.keyFor(selector).cancel();
			this.selector.wakeup();
		}
		eventHandler.handleEvent(new ClientRemoveEvent(sc));
	}

	public synchronized void send(SocketChannel socket, byte[] data) {
		synchronized (this.pendingChanges) {
			// Indicate we want the interest ops set changed
			this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.pendingData) {
				List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList<ByteBuffer>();
					this.pendingData.put(socket, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}

		// Finally, wake up the selecting thread so it can make the required
		// changes
		this.selector.wakeup();
	}

	public void setChannelInfo(SocketChannel channel, ConnectionInfo info) {
		synchronized (this.pendingChanges) {
			pendingChanges.add(new ChangeRequest(channel, ChangeRequest.UPADTEINFO, info));
		}
		// Finally, wake up the selecting thread so it can make the required
		// changes
		this.selector.wakeup();
	}

	public boolean isReady() {
		return ready;
	}

	public void close() {
		closed = true;
		this.selector.wakeup();
	}

	public void run() {
		while (!closed) {
			// Process any pending changes
			synchronized (this.pendingChanges) {
				Iterator<ChangeRequest> changes = this.pendingChanges.iterator();
				while (changes.hasNext()) {
					ChangeRequest change = changes.next();
					if (change.getType() == ChangeRequest.REGISTER) {
						try {
							change.getSocket().register(this.selector, (int) change.getValue());
						} catch (ClosedChannelException e) {
							e.printStackTrace();
						}
						continue;
					}
					SelectionKey key = change.getSocket().keyFor(this.selector);
					if (key == null) {
						eventHandler.handleEvent(new LogEvent("Channel not registered!"));
						continue;
					}
					switch (change.getType()) {
					case ChangeRequest.CHANGEOPS:
						key.interestOps((int) change.getValue());
						break;
					case ChangeRequest.UPADTEINFO:
						key.attach(change.getValue());
						break;
					case ChangeRequest.REGISTER:
						break;
					}
				}
				this.pendingChanges.clear();
			}

			// Wait for an event one of the registered channels
			ready = true;
			try {
				this.selector.select();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ready = false;

			// Iterate over the set of keys for which events are available
			Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
			while (selectedKeys.hasNext()) {
				SelectionKey key = selectedKeys.next();
				selectedKeys.remove();

				if (!key.isValid()) {
					key.cancel();
					eventHandler.handleEvent(new LogEvent("Key became invalid!"));
					continue;
				}
				// Check what event is available and deal with it
				try {
					if (key.isAcceptable()) {
						this.accept(key);
						eventHandler.handleEvent(new LogEvent("Accepted Key!"));
					} else if (key.isReadable()) {
						this.read(key);
						eventHandler.handleEvent(new LogEvent("Reading from Key!"));
					} else if (key.isWritable()) {
						this.write(key);
						eventHandler.handleEvent(new LogEvent("Writing to Key!"));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		// For an accept to be pending the channel must be a server socket
		// channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		// Fire RegisterEvent
		eventHandler.handleEvent(new ClientRegisterEvent(socketChannel));

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(this.selector, SelectionKey.OP_READ);
		System.out.println(socketChannel.getRemoteAddress());
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			eventHandler.handleEvent(new ClientRemoveEvent(socketChannel));
			eventHandler.handleEvent(new LogEvent("Key was removed! (disconnected)"));
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			eventHandler.handleEvent(new ClientRemoveEvent(socketChannel));
			eventHandler.handleEvent(new LogEvent("Key was removed! (disconnected)"));
			return;
		}

		byte[] recBytes = new byte[numRead];
		System.arraycopy(this.readBuffer.array(), 0, recBytes, 0, numRead);
		eventHandler.handleEvent(new DataReceivedEvent(new Data((ConnectionInfo) socketChannel.keyFor(this.selector)
				.attachment(), recBytes)));
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		return socketSelector;
	}

	private void createServerSocketChannel() throws IOException, ClosedChannelException {
		// Create a new non-blocking server socket channel
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.hostPort);
		serverChannel.socket().bind(isa);

		eventHandler.handleEvent(new LogEvent("Now listening on: " + hostAddress + ":" + hostPort));

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
	}

	private class ChangeRequest {
		public static final int REGISTER = 0;
		public static final int CHANGEOPS = 1;
		public static final int UPADTEINFO = 2;

		private SocketChannel socket;
		private int type;
		private Object value;

		public ChangeRequest(SocketChannel socket, int type, Object value) {
			this.socket = socket;
			this.type = type;
			this.value = value;
		}

		public SocketChannel getSocket() {
			return socket;
		}

		public int getType() {
			return type;
		}

		public Object getValue() {
			return value;
		}
	}
}
