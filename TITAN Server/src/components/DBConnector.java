package components;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import objects.Message;
import objects.User;
import frame.LogFrame;

public class DBConnector {

	// JDBC driver name and database URL
	private static final String			JDBC_DRIVER	= "com.mysql.jdbc.Driver";
	private static final String			DB_URL		= "jdbc:mysql://localhost:3306/titan";

	// Database credentials
	private static final String			USER		= "java-server";
	private static final String			PASS		= "xxyababx";

	private static Connection			connection;
	private static PreparedStatement	pstGetMail;
	private static PreparedStatement	pstGetPhone;
	private static PreparedStatement	pstCreateUser;
	private static PreparedStatement	pstGetAllUsers;
	private static PreparedStatement	pstGetUserByID;
	private static PreparedStatement	pstGetUserByUsername;
	private static PreparedStatement	pstGetFriends;
	private static PreparedStatement	pstSetFriends;
	private static PreparedStatement	pstPutMessage;
	private static PreparedStatement	pstGetLastEditedMessage;
	private static PreparedStatement	pstCheckMessages;
	private static PreparedStatement	pstSetRead;
	private static PreparedStatement	pstSetLastOnline;
	private static PreparedStatement	pstSetOnline;
	private static PreparedStatement	pstStatus;
	private static PreparedStatement	pstDeleteAccount;
	private static PreparedStatement	pstGetStatus;

	// Notice: Don't do 'import com.mysql.jdbc.*'
	// Overrides other needed classes!

	public DBConnector() {
		try {
			// The newInstance() call is a work around for some
			// broken Java implementations
			Class.forName(JDBC_DRIVER).newInstance();
			// Connection setup:
			connection = DriverManager.getConnection(DB_URL, USER, PASS);

			//Setup PreparedStatements
			pstGetMail = connection.prepareStatement("SELECT * FROM `email` WHERE ID = ? ;");
			pstGetPhone = connection.prepareStatement("SELECT * FROM `phonenumber` WHERE ID = ? ;");
			pstCreateUser = connection
					.prepareStatement("INSERT INTO `titan`.`users` (`ID`, `Username`, `Password`, `ProfilKey`, `IsOnline`, `LastOnline`, `Status`, `FirstName`, `LastName`, `BirthDate`) VALUES (?, ?, ?, 'PROP', '0', ?, ?, ?, ?, ?);");
			pstGetAllUsers = connection.prepareStatement("SELECT * FROM `users`;");
			pstGetUserByID = connection.prepareStatement("SELECT * FROM `users` WHERE ID = ? ;");
			pstGetUserByUsername = connection.prepareStatement("SELECT * FROM `users` WHERE Username = ? ;");
			pstGetFriends = connection.prepareStatement("SELECT * FROM `contacts` WHERE ID = ? ;");
			pstSetFriends = connection
					.prepareStatement("INSERT INTO titan.contacts (UserID, MateID, Favorite) VALUES (?, ?, NULL);");
			pstPutMessage = connection
					.prepareStatement("INSERT INTO message (Sender,Receiver,Message,MessageRead,MessageTime) VALUES( ?, ?, ?, 0, ?);");
			pstGetLastEditedMessage = connection.prepareStatement("Select LAST_INSERT_ID() FROM titan.message;");
			pstCheckMessages = connection
					.prepareStatement("SELECT * FROM titan.message WHERE MessageRead = 0 AND Receiver = ? ");
			pstSetRead = connection
					.prepareStatement("UPDATE titan.message SET `MessageRead` = '1' WHERE message.ID = ? ");

		} catch (Exception ex) {
			// handle the error

			// or not
			System.err.println("ERROR!!!");
			ex.printStackTrace();
		}
		try {
			connection.setAutoCommit(false);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println("DB ERROR!!!");
			e.printStackTrace();
		}
	}

	/*
	 * public static void main(String[] args) throws SQLException { new
	 * DBConnector(); System.out.println(Arrays.toString(getAllUsers()));
	 * connection.close(); }
	 */

	@SuppressWarnings("unused")
	@Deprecated
	private static ResultSet runStatement(String sql) throws SQLException {
		Statement statement = connection.createStatement();
		if (statement.execute(sql)) {
			ResultSet results = statement.getResultSet();
			results.beforeFirst();
			return results;
		}
		statement.close();
		return null;
	}

	private static User generateUser(ResultSet userSet) throws SQLException {
		String[] mailsArray = getMail(userSet.getInt("ID"));
		String[] phonesArray = getPhoneNumber(userSet.getInt("ID"));

		User user = new User(	userSet.getInt("ID"),
								userSet.getString("Username"),
								userSet.getString("FirstName"),
								userSet.getString("LastName"),
								mailsArray,
								(userSet.getInt("IsOnline") == 1) ? true : false,
								new Date(userSet.getLong("LastOnline")),
								userSet.getString("Status"),
								phonesArray,
								new Date(userSet.getLong("BirthDate")));
		return user;
	}

	private static String[] getMail(int id) throws SQLException {
		pstGetMail.clearParameters();
		pstGetMail.setInt(1, id);
		ResultSet mailSet = pstGetMail.executeQuery();
		connection.commit();
		if (mailSet != null) {
			ArrayList<String> mailsList = new ArrayList<String>();
			while (mailSet.next()) {
				mailsList.add(mailSet.getString("EMail"));
			}
			String[] mailsArray = new String[mailsList.size()];
			mailsArray = mailsList.toArray(mailsArray);

			mailSet.close();

			return mailsArray;
		}
		return new String[0];
	}

	private static String[] getPhoneNumber(int id) throws SQLException {
		pstGetPhone.clearParameters();
		pstGetPhone.setInt(1, id);
		ResultSet phoneSet = pstGetPhone.executeQuery();
		connection.commit();
		if (phoneSet != null) {
			ArrayList<String> phonesList = new ArrayList<String>();
			while (phoneSet.next()) {
				phonesList.add(phoneSet.getString("PhoneNumber"));
			}
			String[] phonesArray = new String[phonesList.size()];
			phonesArray = phonesList.toArray(phonesArray);

			phoneSet.close();

			return phonesArray;
		}
		return new String[0];
	}

	/**
	 * Not needed since registration is done through web. That will change in
	 * beta.
	 * 
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */

	public static void create(int id, User user, char[] password) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
		pstCreateUser.clearParameters();
		pstCreateUser.setInt(1, user.getID());
		pstCreateUser.setString(2, user.getUsername());
		pstCreateUser.setString(3, PasswordHasher.generateMD5(password));
		pstCreateUser.setLong(4, user.getLastOnline().getTime());
		pstCreateUser.setString(5, user.getStatus());
		pstCreateUser.setString(6, user.getFirstName());
		pstCreateUser.setString(7, user.getLastName());
		pstCreateUser.setLong(8, user.getBirthDate().getTime());
		pstCreateUser.executeUpdate();
		connection.commit();
		LogFrame.updateLog("Successfully created User!");
	}

	public static User[] getAllUsers() throws SQLException {
		ResultSet userSet = pstGetAllUsers.executeQuery();
		connection.commit();

		ArrayList<User> usersList = new ArrayList<User>();
		while (userSet.next()) {
			User user = generateUser(userSet);
			usersList.add(user);
		}
		userSet.close();

		User[] usersArray = new User[usersList.size()];
		usersArray = usersList.toArray(usersArray);
		return usersArray;
	}

	public static User get(int id) {
		User user = null;
		try {
			pstGetUserByID.clearParameters();
			pstGetUserByID.setInt(1, id);
			ResultSet set = pstGetUserByID.executeQuery();
			connection.commit();
			set.next();
			user = generateUser(set);
			set.close();
		} catch (SQLException e) {
			// TODO: Auto-generated method stub
			e.printStackTrace();
		}
		return user;
	}

	public static User get(String username) {
		User user = null;
		try {
			pstGetUserByUsername.clearParameters();
			pstGetUserByUsername.setString(1, username);
			ResultSet set = pstGetUserByUsername.executeQuery();
			connection.commit();
			set.next();
			if (set.first()) {
				user = generateUser(set);
			}
			set.close();
		} catch (SQLException e) {
			// TODO: Auto-generated method stub
			e.printStackTrace();
		}
		return user;
	}

	public static User check(String username, char[] password) {
		try {
			pstGetUserByUsername.clearParameters();
			pstGetUserByUsername.setString(1, username);
			ResultSet set = pstGetUserByUsername.executeQuery();
			connection.commit();

			if (set.next()) {
				if (PasswordHasher.validatePassword(password, set.getString("Password"))) {
					return generateUser(set);
				}
			}
			set.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static User[] getFriendsOf(int id) throws SQLException {
		pstGetFriends.clearParameters();
		pstGetFriends.setInt(1, id);
		ResultSet ids = pstGetFriends.executeQuery();
		connection.commit();

		ArrayList<User> usersList = new ArrayList<User>();
		while (ids.next()) {
			User user = get(ids.getInt("MateID"));
			usersList.add(user);
		}
		ids.close();

		User[] usersArray = new User[usersList.size()];
		usersArray = usersList.toArray(usersArray);
		return usersArray;
	}

	public static User setFriends(int firstUser, String secondUsername) {
		User secondUser = get(secondUsername);

		if (secondUser == null) {
			return null;
		}
		try {
			pstSetFriends.clearParameters();
			pstSetFriends.setInt(1, firstUser);
			pstSetFriends.setInt(2, secondUser.getID());

			pstSetFriends.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			try {
				connection.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return secondUser;
	}

	public static int putMessage(int sender, int receiver, String message, long time) throws SQLException {
		pstPutMessage.clearParameters();
		pstPutMessage.setInt(1, sender);
		pstPutMessage.setInt(2, receiver);
		pstPutMessage.setString(3, message);
		pstPutMessage.setLong(4, time / 1000);
		pstPutMessage.executeUpdate();
		connection.commit();

		ResultSet idSet = pstGetLastEditedMessage.executeQuery();
		connection.commit();

		boolean hasID = idSet.next();
		int messageID = 0;

		if (hasID) {
			messageID = idSet.getInt("LAST_INSERT_ID()");
		}
		if (hasID) {
			return messageID;
		} else {
			throw new SQLException("No ID!!!");
		}
	}

	public static Message[] checkUnreadMessages(int receiverId) throws SQLException, ParseException {
		pstCheckMessages.clearParameters();
		pstCheckMessages.setInt(1, receiverId);
		ResultSet set = pstCheckMessages.executeQuery();
		connection.commit();

		ArrayList<Message> messageList = new ArrayList<Message>();

		while (set.next()) {
			messageList.add(new Message(set.getInt("Sender"), set.getInt("Receiver"), set.getString("Message"), set
					.getInt("ID"), new Date(set.getLong("MessageTime") * 1000)));
			pstSetRead.clearParameters();
			pstSetRead.setInt(1, set.getInt("ID"));
			pstSetRead.executeUpdate();
			connection.commit();
		}

		Message[] messageArray = new Message[messageList.size()];
		messageArray = messageList.toArray(messageArray);
		return messageArray;
	}
}
