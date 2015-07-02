package io;

public class ConnectionInfo {
	/**
	 * The program is the Client in this connection.
	 */
	public static final int REMOTEHOSTCONECTION = 1;
	/**
	 * The program is the Server in this connection.
	 */
	public static final int LOCALHOSTCONECTION = 2;
	private int id;
	private int type;

	public ConnectionInfo(int id, int type) throws InvalidTypeException {
		if (type != REMOTEHOSTCONECTION && type != LOCALHOSTCONECTION) {
			throw new InvalidTypeException("Type: " + String.valueOf(type) + " is not a valid Connectiontype.");
		}
		this.id = id;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public int getType() {
		return type;
	}

	public class InvalidTypeException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6655964952031069701L;

		public InvalidTypeException(String message) {
			super(message);
		}
	}
}
