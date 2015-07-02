package objects;

import io.ConnectionInfo;

import java.io.Serializable;

public class Data implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6888334462744644445L;
	private ConnectionInfo info;
	private byte[] data;

	public ConnectionInfo getConnectionInfo() {
		return info;
	}

	public byte[] getBytes() {
		return data;
	}

	public Data(ConnectionInfo info, byte[] array) {
		this.info = info;
		data = array;
	}
}
