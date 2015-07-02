package events;

public class ActionEvent extends Event {
	private String eventString;
	private boolean wasSuccessful;

	public ActionEvent(int eventType, boolean wasSuccessful) {
		this.eventType = eventType;
		this.eventString = null;
		this.wasSuccessful = wasSuccessful;
	}

	public ActionEvent(int eventType, StackTraceElement[] stackTrace) {
		this.eventType = eventType;
		this.wasSuccessful = false;
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement stackTraceElement : stackTrace) {
			sb.append(stackTraceElement.toString());
			sb.append("\n");
		}
		this.eventString = sb.toString();
	}

	public ActionEvent(int eventType, String eventString, boolean wasSuccessful) {
		this.eventType = eventType;
		this.eventString = eventString;
		this.wasSuccessful = wasSuccessful;
	}

	public int getEventType() {
		return eventType;
	}

	public String getEventString() {
		return eventString;
	}

	public boolean wasSuccessful() {
		return wasSuccessful;
	}
}
