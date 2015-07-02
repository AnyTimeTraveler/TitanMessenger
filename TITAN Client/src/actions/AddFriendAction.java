package actions;

public class AddFriendAction extends Action {
	private String username;

	public AddFriendAction(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
