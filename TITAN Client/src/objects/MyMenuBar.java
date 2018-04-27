package objects;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import org.bouncycastle.openpgp.PGPException;

import visual.MessengerFrame;
import components.DataHandler;

public class MyMenuBar extends JMenuBar {
	private JMenu contactsMenu, statusMenu;
	private JRadioButtonMenuItem miOnline, miBusy, miAFK, miOffline;
	private JMenuItem miProfile, miAddFriend, miRemoveFirend, miClose;
	/**
	 * 
	 */
	private static final long serialVersionUID = 2003701585098177105L;

	public MyMenuBar() {
		super();

		// Build status menu
		statusMenu = new JMenu("Status");
		ButtonGroup group = new ButtonGroup();
		miOnline = new JRadioButtonMenuItem("Online");
		miOnline.setSelected(true);
		group.add(miOnline);
		miOnline.addActionListener(new MyActionListener());
		statusMenu.add(miOnline);

		miBusy = new JRadioButtonMenuItem("Busy");
		group.add(miBusy);
		miBusy.addActionListener(new MyActionListener());
		statusMenu.add(miBusy);

		miAFK = new JRadioButtonMenuItem("AFK");
		group.add(miAFK);
		miAFK.addActionListener(new MyActionListener());
		statusMenu.add(miAFK);

		miOffline = new JRadioButtonMenuItem("Offline");
		group.add(miOffline);
		miOffline.addActionListener(new MyActionListener());
		statusMenu.add(miOffline);

		// Build contacts menu
		contactsMenu = new JMenu("Contacts");

		// TODO Not ready!
		//
		// miProfile = new JMenuItem("View my Profile");
		// miProfile.addActionListener(new MyActionListener());
		// contactsMenu.add(miProfile);

		miAddFriend = new JMenuItem("Add a Friend...");
		miAddFriend.addActionListener(new MyActionListener());
		contactsMenu.add(miAddFriend);

		miRemoveFirend = new JMenuItem("Remove selected Friend");
		miRemoveFirend.addActionListener(new MyActionListener());
		contactsMenu.add(miRemoveFirend);

		// miClose = new JMenuItem("Close");
		// miClose.addActionListener(new MyActionListener());
		// contactsMenu.add(miClose);

		this.add(statusMenu);
		this.add(contactsMenu);
	}

	private class MyActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(miAddFriend)) {
				String username = JOptionPane.showInputDialog("Please enter your friends Username:");
				if (username != null && username != "") {
					try {
						DataHandler.addFriend(username);
					} catch (IOException | InterruptedException | DataFormatException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (PGPException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			} else if (e.getSource().equals(miRemoveFirend)) {
				try {
					MessengerFrame.getInstance().removeSelectedUser();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				MessengerFrame.getInstance().updateContactList();
			} else if (e.getSource().equals(miProfile)) {
				// TODO Add a Frame for the users own profile.
			}
		}
	}
}
