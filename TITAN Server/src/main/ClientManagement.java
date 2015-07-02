package main;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import frame.LogFrame;

public class ClientManagement {

	private static int								clients	= 0;
	private static BidiMap<Integer, SocketChannel>	clientMap;
	private static final int						tempIDs	= 1000;
	private static boolean[]						tempIDAvailability;

	public ClientManagement() {

		clientMap = new DualHashBidiMap<Integer, SocketChannel>();
		tempIDAvailability = new boolean[tempIDs];
		Arrays.fill(tempIDAvailability, true);
	}

	public static int getClientCount() {
		return clients;
	}

	public static Set<Integer> getAllIDs() {
		return clientMap.keySet();
	}

	public static boolean isOnline(int id) {

		return clientMap.containsKey(id);
	}

	private synchronized static void clientDisconnected() {
		clients--;
		LogFrame.updateLog("Current amount of clients : " + clients);
	}

	private synchronized static void clientConnected() {
		clients++;
		LogFrame.updateLog("Current amount of clients : " + clients);
	}

	public synchronized static void register(SocketChannel channel) {
		clientConnected();
		int tempID = getTempChannel();
		if (tempID != -1) {
			clientMap.put(tempID, channel);
		} else {
			throw new ArrayIndexOutOfBoundsException();
		}
	}

	// Unused (yet)
	public synchronized static void register(int id, SocketChannel channel) {
		clientConnected();
		clientMap.put(id, channel);
	}

	// Unused (yet)
	public synchronized static void remove(int id) {
		clientDisconnected();
		clientMap.remove(id);
	}

	public synchronized static int remove(SocketChannel channel) {
		clientDisconnected();
		return clientMap.removeValue(channel);
	}

	public synchronized static void changeID(int oldID, int newID) {
		clientMap.put(newID, clientMap.remove(oldID));
	}

	public synchronized static SocketChannel getChannel(int id) {
		return clientMap.get(id);
	}

	public synchronized static int getID(SocketChannel channel) {
		return clientMap.getKey(channel);
	}

	private synchronized static int getTempChannel() {
		int id = -1;
		for (int i = 0; i < tempIDAvailability.length; i++) {
			if (tempIDAvailability[i]) {
				id = i;
				break;
			}
		}
		return id;
	}
}
