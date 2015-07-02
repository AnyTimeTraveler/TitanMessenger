package frame;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import main.MainServer;
import objects.User;

import components.DBConnector;

public class LogFrame extends JFrame {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private JPanel				contentPane;
	private JTextField			textField;
	private static JTextArea	textArea;
	private static boolean		showFrame			= false;
	private JScrollPane			scrollPane;
	private boolean				genuser				= false;

	// TempFields
	private int					ID;
	private String				username;
	private char[]				password;
	private String				firstname;
	private String				lastname;
	private boolean				isOnline			= false;
	private java.util.Date		lastOnline;
	private String				status;
	private java.util.Date		birthDate;
	private String[]			emails;
	private String[]			phoneNumbers;

	/**
	 * Create the frame.
	 */
	public LogFrame() {
		super("Server");
		showFrame = true;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setAlwaysOnTop(true);
		setBounds(1000, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		textField = new JTextField();
		panel.add(textField, BorderLayout.CENTER);
		textField.setColumns(10);

		InputMap iMap = textField.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap aMap = textField.getActionMap();

		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
		aMap.put("enter", new AbstractAction() {

			/**
			 * 
			 */
			private static final long	serialVersionUID	= 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				runCommand();
			}
		});

		JButton btnExecute = new JButton("Execute");
		btnExecute.addActionListener(new MyActionListener());
		panel.add(btnExecute, BorderLayout.EAST);

		scrollPane = new JScrollPane();
		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		setVisible(true);
	}

	private void runCommand() {
		String[] command = textField.getText().split(" ");
		textField.setText("");
		lastOnline = new java.util.Date();
		if (genuser) {
			if (command[0].equals("q") || command[0].equals("quit") || command[0].equals("cancel") ||
					command[0].equals("exit")) {
				ID = 0;
				username = null;
				password = null;
				firstname = null;
				lastname = null;
				lastOnline = null;
				status = null;
				birthDate = null;
				emails = null;
				phoneNumbers = null;
				genuser = false;
			}
			if (ID == 0) {
				ID = Integer.valueOf(command[0]);
				LogFrame.updateLog("Please enter the Username:");
				return;
			}
			if (username == null) {
				username = command[0];
				LogFrame.updateLog("Please enter the Password:");
				return;
			}
			if (password == null) {
				password = command[0].toCharArray();
				LogFrame.updateLog("Please enter the first Name:");
				return;
			}
			if (firstname == null) {
				firstname = command[0];
				LogFrame.updateLog("Please enter the last Name:");
				return;
			}
			if (lastname == null) {
				lastname = command[0];
				LogFrame.updateLog("Please enter the initial Status:");
				return;
			}
			if (status == null) {
				status = command[0];
				LogFrame.updateLog("Please enter the Birthdate:");
				LogFrame.updateLog("(yyyy-mm-dd)");
				return;
			}
			if (birthDate == null) {
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				try {
					birthDate = df.parse(command[0]);
				} catch (ParseException ex) {
					LogFrame.updateLog("Dateparsing failed. Please look at the format.");
					return;
				}
				LogFrame.updateLog("Please enter the an Emailaddress:");
				return;
			}
			if (emails == null) {
				emails = new String[1];
				emails[0] = command[0];
				LogFrame.updateLog("Please enter the Phonenumber:");
				return;
			}
			if (phoneNumbers == null) {
				phoneNumbers = new String[1];
				phoneNumbers[0] = command[0];
			}
			try {
				DBConnector.create(ID, new User(ID,
												username,
												firstname,
												lastname,
												emails,
												isOnline,
												lastOnline,
												status,
												phoneNumbers,
												birthDate), password);
				updateLog("Done! Generated User!");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				updateLog(e.getMessage());
				e.printStackTrace();
			}

			ID = 0;
			username = null;
			password = null;
			firstname = null;
			lastname = null;
			lastOnline = null;
			status = null;
			birthDate = null;
			emails = null;
			phoneNumbers = null;
			genuser = false;
			return;
		}
		switch (command[0].toLowerCase()) {
			case "create":
			case "createuser":
				genuser = true;
				updateLog("Please enter ID:");
				break;
			case "exit":
			case "quit":
			case "q":
			case "leave":
			case "close":
				updateLog("Shutting down...");
				MainServer.getWorker().close();
				this.dispose();
				break;
			case "send":
				if (command.length != 4) {
					updateLog("send <Message> <senderID> <receiverID>");
				} else {
					String message = command[1];
					int senderID = Integer.parseInt(command[2]);
					int receiverID = Integer.parseInt(command[3]);

					try {
						DBConnector.putMessage(senderID, receiverID, message, System.currentTimeMillis());
						updateLog("Messange has been sent!");
						updateLog(senderID + " -> " + receiverID + " : " + message);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						updateLog("Sending failed!");
						updateLog(e);
					}
				}
				break;
			default:
				StringBuilder sb = new StringBuilder();
				for (String string : command) {
					sb.append(string);
					sb.append(" ");
				}
				updateLog("Unknown Command: " + sb.toString());
				break;
		}
	}

	public static void updateLog(Throwable t) {
		updateLog(t.toString());
		for (StackTraceElement stackTraceElement : t.getStackTrace()) {
			updateLog("    " + stackTraceElement.toString());
		}
		t.printStackTrace();
	}

	public synchronized static void updateLog(String newLine) {
		if (showFrame) {
			textArea.setText(textArea.getText() + "\n" + newLine);
			textArea.setCaretPosition(textArea.getDocument().getLength());
		} else {
			System.out.println(newLine);
		}
	}

	private class MyActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub

		}

	}
}
