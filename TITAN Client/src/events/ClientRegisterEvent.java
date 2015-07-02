package events;

import java.nio.channels.SocketChannel;

public class ClientRegisterEvent extends Event {
	private SocketChannel socketChannel;

	public ClientRegisterEvent(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}
}
