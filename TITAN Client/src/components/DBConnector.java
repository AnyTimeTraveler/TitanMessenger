package components;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import main.Config;
import objects.User;

public class DBConnector {

	// JDBC driver name and database URL
	private static final String	JDBC_DRIVER	= "org.sqlite.JDBC";
	private static final String	DB_URL		= "jdbc:sqlite:";

	private static Connection	connection;
	private static boolean		connected	= false;

	// Notice don't do 'import com.mysql.jdbc.*'
	// Overrides other needed classes!

	public DBConnector() {
		try {
			// The newInstance() call is a work around for some
			// broken Java implementations
			Class.forName(JDBC_DRIVER).newInstance();

		} catch (Exception ex) {
			// handle the error

			// or not
			ex.printStackTrace();
		}
	}

	public static void connect(String username) {
		boolean exists = new File(username + ".db").exists();

		if (!connected) {
			// Connection setup:
			try {
				connection = DriverManager.getConnection(DB_URL + username + ".db");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				connected = false;
				e.printStackTrace();
			}
			connected = true;
		}
		if (!exists) {
			String[] sql = new String[] {
					"CREATE TABLE Friends (IsFavorite NUMERIC, ID INTEGER PRIMARY KEY, Username TEXT, FirstName TEXT, LastName TEXT, IsOnline NUMERIC, LastOnline TEXT, Status TEXT, BirthDate TEXT);",
					"CREATE TABLE Mail (ID NUMERIC, EMail TEXT);",
					"CREATE TABLE Messages (Date NUMERIC, Message TEXT, Receiver NUMERIC, Sender NUMERIC);",
					"CREATE TABLE Phonenumbers (ID NUMERIC, Number TEXT);" };
			for (String string : sql) {
				try {
					Statement statement = connection.createStatement();
					statement.executeUpdate(string);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	/*
	 * public static void main(String[] args) throws SQLException { new
	 * DBConnector(); System.out.println(Arrays.toString(getAllUsers()));
	 * connection.close(); }
	 */

	private static ResultSet runStatement(String sql) throws SQLException {
		Statement statement = connection.createStatement();
		if (statement.execute(sql)) {
			return statement.getResultSet();
		}
		statement.close();
		return null;
	}

	public static User[] getContactList() throws SQLException {
		ResultSet userSet = runStatement("SELECT * FROM `Friends`;");

		ArrayList<User> usersList = new ArrayList<User>();
		while (userSet.next()) {
			usersList.add(generateUser(userSet));
		}
		userSet.getStatement().close();
		userSet.close();

		User[] users = new User[usersList.size()];
		users = usersList.toArray(users);
		return users;
	}

	private static User generateUser(ResultSet userSet) throws SQLException {
		String[] mailsArray = getMail(userSet.getInt("ID"));
		String[] phonesArray = getPhoneNumber(userSet.getInt("ID"));

		Date lastOnline = new Date();
		Date birthDate = new Date();
		lastOnline.setTime(userSet.getLong("LastOnline"));
		birthDate.setTime(userSet.getLong("BirthDate"));

		User user = new User(	userSet.getInt("ID"),
								userSet.getString("Username"),
								userSet.getString("FirstName"),
								userSet.getString("LastName"),
								mailsArray,
								(userSet.getInt("IsOnline") == 1) ? true : false,
								lastOnline,
								userSet.getString("Status"),
								phonesArray,
								birthDate);
		return user;
	}

	private static String[] getMail(int id) throws SQLException {
		ResultSet mailSet = runStatement("SELECT * FROM `Mail` WHERE ID = " + id + ";");
		ArrayList<String> mailsList = new ArrayList<String>();

		if (mailSet == null) {
			return null;
		}

		while (mailSet.next()) {
			mailsList.add(mailSet.getString("EMail"));
		}
		String[] mailsArray = new String[mailsList.size()];
		mailsArray = mailsList.toArray(mailsArray);

		mailSet.getStatement().close();
		mailSet.close();

		return mailsArray;
	}

	private static String[] getPhoneNumber(int id) throws SQLException {
		ResultSet phoneSet = runStatement("SELECT * FROM `Phonenumbers` WHERE ID = " + Integer.valueOf(id) + ";");

		if (phoneSet == null) {
			return null;
		}

		ArrayList<String> phonesList = new ArrayList<String>();
		while (phoneSet.next()) {
			phonesList.add(phoneSet.getString("Number"));
		}
		String[] phonesArray = new String[phonesList.size()];
		phonesArray = phonesList.toArray(phonesArray);

		phoneSet.getStatement().close();
		phoneSet.close();

		return phonesArray;
	}

	/**
	 * Not needed since registration is done through web. That will change in
	 * beta.
	 * 
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */

	public static void addToContacts(User user) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
		String[] data = new String[9];
		data[0] = "0";
		data[1] = String.valueOf(user.getID());
		data[2] = user.getUsername();
		data[3] = user.getFirstName();
		data[4] = user.getLastName();
		data[5] = String.valueOf(user.isOnline() ? 1 : 0);
		data[6] = String.valueOf(user.getLastOnline().getTime());
		data[7] = user.getStatus();
		data[8] = String.valueOf(user.getBirthDate().getTime());

		StringBuilder sb = new StringBuilder();

		sb.append("INSERT INTO `Friends` VALUES ('");
		for (int i = 0; i < data.length; i++) {
			String item = data[i];
			sb.append(item);
			if (i < (data.length - 1)) {
				sb.append("' , '");
			} else {
				sb.append("');");
			}
		}
		String sql = sb.toString();
		runStatement(sql);
		System.out.println("Successfully created User!");
	}

	public static User get(int id) {
		User user = null;
		try {
			ResultSet set = runStatement("SELECT * FROM `Friends` WHERE ID = " + id + ";");
			set.next();
			user = generateUser(set);
			set.getStatement().close();
			set.close();
		} catch (SQLException e) {
			// TODO: Auto-generated method stub
			e.printStackTrace();
		}
		return user;
	}

	public static void removeFromContacts(User user) throws SQLException {
		removeFromContacts(user.getID());
	}

	public static void removeFromContacts(int id) throws SQLException {
		runStatement("DELETE FROM `Friends` WHERE ID = " + id + ";");
	}

	public static String getChatHistory(int userId, User[] contacts) throws SQLException {
		ResultSet messageSet = runStatement("SELECT * FROM `Messages` WHERE Sender = '" + userId + "' OR Receiver = '" +
				userId + "' LIMIT 5000;");
		DateFormat df = new SimpleDateFormat("[HH:mm:ss]");
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		for (User user : contacts) {
			map.put(user.getID(), user.getUsername());
		}
		map.put(Config.getInstance().thisUser.getID(), Config.getInstance().thisUser.getUsername());
		StringBuilder sb = new StringBuilder();
		while (messageSet.next()) {
			String message = map.get(messageSet.getInt("Sender")) + " " +
					df.format(new Date(messageSet.getLong("Date"))) + " : " + messageSet.getString("Message");
			sb.append(message + "\n");
		}
		return sb.toString();
	}

	public static void addToChatHistory(int sender, int receiver, String message) throws SQLException {
		runStatement("INSERT INTO `Messages` (Sender,Receiver,Message,Date) VALUES('" + sender + "' , '" + receiver +
				"' , '" + message + "' , '" + System.currentTimeMillis() + "');");
	}

	public static void close() throws SQLException {
		connection.close();
	}
}
