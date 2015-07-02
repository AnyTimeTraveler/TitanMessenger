package objects;

public class PacketType {
	public static final int LoginPacket = 1;
	public static final int StatusPacket = 2;
	public static final int MessagePacket = 3;
	public static final int UserPacket = 4;
	public static final int RequestPacket = 5;
	public static final int KeyPacket = 6;

	public static String getType(int packetType) {
		// TODO Auto-generated method stub
		switch (packetType) {
		case LoginPacket:
			return "LoginPacket";
		case StatusPacket:
			return "StatusPacket";
		case MessagePacket:
			return "MessagePacket";
		case UserPacket:
			return "UserPacket";
		case RequestPacket:
			return "RequestPacket";
		case KeyPacket:
			return "KeyPacket";
		default:
			return "UnknownPacket";

		}
	}
}
