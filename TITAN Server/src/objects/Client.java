package objects;

import java.nio.channels.SocketChannel;

public class Client {
	public SocketChannel getChannel() {
		return channel;
	}

	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}

	public boolean isLoggedin() {
		return loggedin;
	}

	public void setLoggedin(boolean loggedin) {
		this.loggedin = loggedin;
	}

	private SocketChannel channel;
	private boolean loggedin;

	// private PGPKey key;

	public Client(SocketChannel channel) {
		this.channel = channel;
	}

	public Client(SocketChannel channel, boolean isloggedin) {
		this.channel = channel;
		this.loggedin = isloggedin;
	}

	public Client(boolean isloggedin) {
		this.loggedin = isloggedin;
	}
}
