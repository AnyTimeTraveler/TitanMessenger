package events;

import objects.Data;

public class DataReceivedEvent extends Event {

	private Data data;

	public DataReceivedEvent(Data data) {
		this.data = data;
	}

	public Data getData() {
		return data;
	}

}
