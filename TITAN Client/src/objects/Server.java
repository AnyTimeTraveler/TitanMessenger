package objects;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import main.Config;

public class Server implements Serializable {
	private InetAddress address;
	private String name;
	private int port;

	public Server(String name, String address, int port) throws UnknownHostException {
		this.name = name;
		this.address = InetAddress.getByName(address);
		this.port = port;
	}

	public Server(String name, InetAddress address, int port) throws UnknownHostException {
		this.name = name;
		this.address = address;
		this.port = port;
	}

	public void setAddress(String address) throws UnknownHostException {
		this.address = InetAddress.getByName(address);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + port;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Server other = (Server) obj;
		if (address == null) {
			if (other.address != null) {
				return false;
			}
		} else if (!address.equals(other.address)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Server [address=" + address + ", name=" + name + ", port=" + port + "]";
	}

	public static Server getByName(String string) throws UnknownHostException {
		for (Server server : Config.getInstance().servers) {
			if (server.name.equals(string)) {
				return server;
			}
		}
		return new Server("", "", 0);
	}

}
