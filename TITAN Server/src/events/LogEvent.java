package events;

public class LogEvent extends Event {
	private String message;

	public LogEvent(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
