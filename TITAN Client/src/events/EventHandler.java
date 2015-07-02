package events;

public interface EventHandler extends Runnable {

	void handleEvent(Event event);

}
