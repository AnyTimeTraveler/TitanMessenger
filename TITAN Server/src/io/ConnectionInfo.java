package io;

import org.bouncycastle.openpgp.PGPPublicKey;

public class ConnectionInfo {

	/**
	 * The program is the Client in this connection.
	 */
	public static final int	REMOTEHOSTCONECTION	= 1;
	/**
	 * The program is the Server in this connection.
	 */
	public static final int	LOCALHOSTCONECTION	= 2;
	private int				id;
	private int				type;
	private boolean			loggedIn;
	private boolean			readyToReceiveMessages;
	private boolean			encrypted;
	private PGPPublicKey	key;

	public ConnectionInfo(int id) {
		this.id = id;
		loggedIn = false;
		setReadyToReceiveMessages(false);
		encrypted = false;
		key = null;
	}

	public ConnectionInfo(int id, int type) {
		this.id = id;
		this.type = type;
		loggedIn = false;
		setReadyToReceiveMessages(false);
		encrypted = false;
		key = null;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public PGPPublicKey getKey() {
		return key;
	}

	public void setKey(PGPPublicKey key) {
		this.key = key;
	}

	public boolean isReadyToReceiveMessages() {
		return readyToReceiveMessages;
	}

	public void setReadyToReceiveMessages(boolean readyToReceiveMessages) {
		this.readyToReceiveMessages = readyToReceiveMessages;
	}
}
