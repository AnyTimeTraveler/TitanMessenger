package events;

public abstract class Event {

	public static final int Connect = 1;
	public static final int SendClientKey = 2;
	public static final int RetrieveServerKey = 3;
	public static final int Login = 4;

	protected int eventType = 0;
}
