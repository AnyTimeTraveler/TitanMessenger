package events;

import java.nio.channels.SocketChannel;

public class ClientRemoveEvent extends Event {

	private SocketChannel	socketChannel;

	public ClientRemoveEvent(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

}
