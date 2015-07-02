package events;

import java.nio.channels.SocketChannel;

public class DataReceivedEvent extends Event {

	private byte[] bytes;
	private SocketChannel socketChannel;

	public DataReceivedEvent(SocketChannel socketChannel, byte[] bytes) {
		this.bytes = bytes;
		this.socketChannel = socketChannel;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

}
