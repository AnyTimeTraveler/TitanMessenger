package objects;

import java.util.Date;

public class Message {
	private String content;
	private int sender;
	private int receiver;
	private Date time;
	private int ID;

	public Message() {

	}

	public Message(User sender, User receiver, String content, int id, Date time) {
		this.content = content;
		this.sender = sender.getID();
		this.receiver = receiver.getID();
		this.ID = id;
		this.time = time;
	}

	public Message(int sender, int receiver, String content, int id, Date time) {
		this.content = content;
		this.sender = sender;
		this.receiver = receiver;
		this.ID = id;
		this.time = time;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getSender() {
		return sender;
	}

	public void setSender(int sender) {
		this.sender = sender;
	}

	public int getReceiver() {
		return receiver;
	}

	public void setReceiver(int receiver) {
		this.receiver = receiver;
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

}
