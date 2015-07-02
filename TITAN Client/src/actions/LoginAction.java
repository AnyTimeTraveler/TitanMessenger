package actions;

import java.util.Observer;

import objects.Server;

public class LoginAction extends Action {
	private Server server;
	private String username;
	private char[] password;
	private Observer observer;

	public LoginAction(Server server, String username, char[] password, Observer observer) {
		this.server = server;
		this.username = username;
		this.password = password;
		this.observer = observer;
	}

	public Server getServer() {
		return server;
	}

	public String getUsername() {
		return username;
	}

	public char[] getPassword() {
		return password;
	}

	public Observer getObserver() {
		return observer;
	}

}
