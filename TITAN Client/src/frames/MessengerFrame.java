package frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Config;
import objects.Message;
import objects.MyMenuBar;
import objects.User;

import org.bouncycastle.openpgp.PGPException;

import components.DBConnector;
import components.DataHandler;

public class MessengerFrame extends JFrame {

	/**
	 * 
	 */
	private static final long				serialVersionUID	= -7582245264055107600L;
	private static MessengerFrame			instance;
	private JPanel							contentPane;
	private JPanel							textInput;
	private JTextField						input;
	private JButton							button;
	private User[]							users;
	private LinkedBlockingQueue<Message>	messageQueue;
	private JScrollPane						scrollPane;
	private JTextArea						textArea;
	private JList<String>					list;
	private JSplitPane						splitPane;
	private JPanel							pChatHistory;
	private JPanel							pContacts;
	private MyMenuBar						menuBar;
	private boolean							closed;
	private JLabel							lblContactInfo;

	public static MessengerFrame getInstance() {
		return instance;
	}

	/**
	 * Create the frame.
	 * 
	 * @param receiveQueue
	 * @param messageQueue
	 */
	public MessengerFrame() {
		super("TITAN Messenger");
		instance = this;
		this.messageQueue = DataHandler.getMainFrameQueue();
		Thread receiverBridge = new Thread(new ReceiverBridge());
		receiverBridge.setName("ReceiverBridge");
		receiverBridge.setDaemon(true);
		receiverBridge.start();

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setBounds(200, 200, 539, 355);
		menuBar = new MyMenuBar();
		setJMenuBar(menuBar);
		addWindowListener(new MyWindowListener());
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		splitPane = new JSplitPane();
		contentPane.add(splitPane, BorderLayout.CENTER);

		pChatHistory = new JPanel();
		splitPane.setRightComponent(pChatHistory);
		pChatHistory.setLayout(new BorderLayout(0, 0));

		textInput = new JPanel();
		pChatHistory.add(textInput, BorderLayout.SOUTH);
		textInput.setLayout(new BorderLayout());
		input = new JTextField();
		input.setToolTipText("Geben Sie hier ihre Nachricht ein!");
		input.setFont(new Font("Arial", Font.PLAIN, 11));
		input.setBackground(Color.LIGHT_GRAY);
		input.setBounds(100, 100, 250, 250);
		input.setColumns(10);

		InputMap iMap = input.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap aMap = input.getActionMap();

		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
		aMap.put("enter", new AbstractAction() {

			/**
			 * 
			 */
			private static final long	serialVersionUID	= -6077399116133684717L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				sendMessage();
			}
		});
		textInput.setBorder(new EmptyBorder(5, 5, 5, 5));
		textInput.add(input, BorderLayout.CENTER);

		button = new JButton();
		button.setText("Senden");
		button.addActionListener(new MyActionListener());
		textInput.add(button, BorderLayout.EAST);

		scrollPane = new JScrollPane();
		pChatHistory.add(scrollPane, BorderLayout.CENTER);

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		lblContactInfo = new JLabel("Please chose a contact to chat with!");

		textArea.setLayout(new BorderLayout());
		textArea.add(lblContactInfo, BorderLayout.CENTER);
		scrollPane.setViewportView(textArea);

		pContacts = new JPanel();
		splitPane.setLeftComponent(pContacts);
		pContacts.setLayout(new BorderLayout(0, 0));

		list = new JList<String>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		updateContactList();
		pContacts.add(list);
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {

				if (list.getSelectedIndex() == -1 || users.length <= list.getSelectedIndex()) {
					textArea.setText("");
					lblContactInfo.setVisible(true);
					return;
				}

				try {
					lblContactInfo.setVisible(false);
					textArea.setText(DBConnector.getChatHistory(users[list.getSelectedIndex()].getID(), users));
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});
		this.setVisible(true);
	}

	public void removeSelectedUser() throws SQLException {
		DataHandler.removeFriend(users[list.getSelectedIndex()].getID());
	}

	public void updateContactList() {
		int selection = list.getSelectedIndex();
		User[] contactData = new User[] {};
		try {
			contactData = DBConnector.getContactList();
			users = contactData;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String[] names = new String[contactData.length];
		for (int i = 0; i < contactData.length; i++) {
			names[i] = contactData[i].getUsername();
		}
		list.setListData(names);
		list.setSelectedIndex(selection);
	}

	private void updateChatHistory(Message message) {
		try {
			textArea.setText(DBConnector.getChatHistory(message.getReceiver(), users));
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
	}

	private void sendMessage() {
		if (list.getSelectedIndex() == -1) {
			return;
		}
		User receiver = DBConnector.get(users[list.getSelectedIndex()].getID());
		// TODO: Set the id on contact list
		try {
			DataHandler.sendMessage(users[list.getSelectedIndex()].getID(), input.getText(), new Date());
			DBConnector.addToChatHistory(Config.getInstance().thisUser.getID(),
				users[list.getSelectedIndex()].getID(),
				input.getText());
		} catch (IOException | PGPException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		updateChatHistory(new Message(Config.getInstance().thisUser, receiver, input.getText(), 0, new Date()));
		input.setText("");
		input.requestFocusInWindow();
	}

	private class MyActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			sendMessage();
		}
	}

	private class MyWindowListener implements WindowListener {

		@Override
		public void windowActivated(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosed(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosing(WindowEvent arg0) {
			// TODO Auto-generated method stub
			System.out.println("Closing Window and saving config!");
			Config.save();
			closed = true;
			DataHandler.close();
			try {
				DBConnector.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			MessengerFrame.this.dispose();
		}

		@Override
		public void windowDeactivated(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeiconified(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowIconified(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowOpened(WindowEvent arg0) {
			// TODO Auto-generated method stub

		}
	}

	private class ReceiverBridge implements Runnable {

		@Override
		public void run() {
			while (!closed) {
				Message received = null;

				try {
					received = MessengerFrame.this.messageQueue.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (received == null) {
					continue;
				}

				// case PacketType.MessagePacket:
				updateChatHistory(received);
				// break;
				// case PacketType.StatusPacket:
				// System.out.println("StatusPacket received!");
				// break;
				// default:
				// System.out.println("UnknownPacket received!");
				// break;

				MessengerFrame.getInstance().updateContactList();
			}
		}
	}
}
