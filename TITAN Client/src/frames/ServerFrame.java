package frames;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.UnknownHostException;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Config;
import objects.Server;

public class ServerFrame extends JFrame {

	private JPanel contentPane;
	private JTextField tfName;
	private JTextField tfAddress;
	private JTextField tfPort;
	private JList<String> listServers;
	private JButton btnSave;
	private JButton btnAdd;
	private JButton btnRemove;
	private String[] servers;
	private LoginFrame loginFrame;
	private ServerFrame instance;

	/**
	 * Create the frame.
	 * 
	 * @param instance
	 */
	public ServerFrame(LoginFrame instance) {
		super("Edit Serverlist");
		this.loginFrame = instance;
		this.instance = this;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setBounds(new Rectangle(500, 300));
		setMinimumSize(new Dimension(500, 300));
		setPreferredSize(new Dimension(500, 300));
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

		JPanel pList = new JPanel();
		contentPane.add(pList);
		pList.setLayout(new BorderLayout(0, 0));

		listServers = new JList<String>();
		listServers.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				Server server = null;
				try {
					server = Server.getByName(listServers.getSelectedValue());
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tfAddress.setText(server.getAddress().toString());
				tfName.setText(server.getName());
				tfPort.setText(String.valueOf(server.getPort()));
			}
		});
		updateServerList();
		pList.add(listServers);

		JPanel pSettings = new JPanel();
		contentPane.add(pSettings);

		JLabel lblName = new JLabel("Server Name");

		tfName = new JTextField();
		lblName.setLabelFor(tfName);
		tfName.setColumns(10);

		JLabel lblAddress = new JLabel("Address");

		tfAddress = new JTextField();
		lblAddress.setLabelFor(tfAddress);
		tfAddress.setColumns(10);

		JLabel lblPort = new JLabel("Port");

		tfPort = new JTextField();
		lblPort.setLabelFor(tfPort);
		tfPort.setColumns(10);

		btnSave = new JButton("Save");
		btnSave.addActionListener(new MyActionListener());

		btnAdd = new JButton("+");
		btnAdd.addActionListener(new MyActionListener());

		btnRemove = new JButton("-");
		btnRemove.addActionListener(new MyActionListener());

		GroupLayout gl_pSettings = new GroupLayout(pSettings);
		gl_pSettings
				.setHorizontalGroup(gl_pSettings
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_pSettings
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												gl_pSettings
														.createParallelGroup(Alignment.TRAILING)
														.addGroup(
																gl_pSettings
																		.createSequentialGroup()
																		.addComponent(tfName, GroupLayout.DEFAULT_SIZE,
																				308, Short.MAX_VALUE).addContainerGap())
														.addGroup(
																gl_pSettings
																		.createSequentialGroup()
																		.addGroup(
																				gl_pSettings
																						.createParallelGroup(
																								Alignment.LEADING)
																						.addComponent(lblName)
																						.addComponent(lblAddress)
																						.addComponent(
																								tfAddress,
																								GroupLayout.DEFAULT_SIZE,
																								203, Short.MAX_VALUE)
																						.addGroup(
																								gl_pSettings
																										.createSequentialGroup()
																										.addGroup(
																												gl_pSettings
																														.createParallelGroup(
																																Alignment.TRAILING,
																																false)
																														.addComponent(
																																btnRemove,
																																Alignment.LEADING,
																																GroupLayout.DEFAULT_SIZE,
																																GroupLayout.DEFAULT_SIZE,
																																Short.MAX_VALUE)
																														.addComponent(
																																btnAdd,
																																Alignment.LEADING,
																																GroupLayout.DEFAULT_SIZE,
																																GroupLayout.DEFAULT_SIZE,
																																Short.MAX_VALUE))
																										.addPreferredGap(
																												ComponentPlacement.RELATED,
																												159,
																												Short.MAX_VALUE)))
																		.addGroup(
																				gl_pSettings
																						.createParallelGroup(
																								Alignment.TRAILING)
																						.addGroup(
																								gl_pSettings
																										.createSequentialGroup()
																										.addGap(3)
																										.addComponent(
																												btnSave,
																												GroupLayout.PREFERRED_SIZE,
																												102,
																												GroupLayout.PREFERRED_SIZE)
																										.addGap(12))
																						.addGroup(
																								gl_pSettings
																										.createSequentialGroup()
																										.addGroup(
																												gl_pSettings
																														.createParallelGroup(
																																Alignment.LEADING)
																														.addComponent(
																																lblPort)
																														.addComponent(
																																tfPort,
																																GroupLayout.PREFERRED_SIZE,
																																93,
																																GroupLayout.PREFERRED_SIZE))
																										.addContainerGap()))))));
		gl_pSettings.setVerticalGroup(gl_pSettings.createParallelGroup(Alignment.LEADING).addGroup(
				gl_pSettings
						.createSequentialGroup()
						.addContainerGap()
						.addComponent(lblName)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(tfName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addGap(18)
						.addGroup(
								gl_pSettings.createParallelGroup(Alignment.BASELINE).addComponent(lblAddress)
										.addComponent(lblPort))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(
								gl_pSettings
										.createParallelGroup(Alignment.BASELINE)
										.addComponent(tfAddress, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(tfPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
						.addGroup(
								gl_pSettings
										.createParallelGroup(Alignment.TRAILING)
										.addComponent(btnSave)
										.addGroup(
												gl_pSettings.createSequentialGroup().addComponent(btnAdd).addGap(6)
														.addComponent(btnRemove))).addContainerGap()));
		pSettings.setLayout(gl_pSettings);
		addWindowListener(new MyWindowListener());
		setVisible(true);
	}

	private void updateServerList() {
		servers = new String[Config.getInstance().servers.length];
		for (int i = 0; i < Config.getInstance().servers.length; i++) {
			servers[i] = Config.getInstance().servers[i].getName();
		}
		listServers.setListData(servers);
	}

	private class MyActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(btnAdd)) {
				String[] serversNew = new String[servers.length + 1];
				System.arraycopy(servers, 0, serversNew, 0, servers.length);
				serversNew[servers.length] = "New";
				servers = serversNew;
				listServers.setListData(servers);
				listServers.setSelectedIndex(servers.length - 1);
			} else if (e.getSource().equals(btnRemove)) {
				String[] serversNew = new String[servers.length - 1];
				int listCounter = 0;
				for (int i = 0; i < servers.length; i++) {
					if (!servers[i].equals(listServers.getSelectedValue())) {
						serversNew[listCounter] = servers[i];
						listCounter++;
					}
				}
				servers = serversNew;
				listServers.setListData(servers);
			} else if (e.getSource().equals(btnSave)) {
				Server[] serversToStore = new Server[servers.length];
				for (int i = 0; i < servers.length; i++) {

					if (i == listServers.getSelectedIndex()) {
						try {
							serversToStore[i] = new Server(tfName.getText(), tfAddress.getText(),
									Integer.valueOf(tfPort.getText()));
						} catch (NumberFormatException | UnknownHostException e1) {
							e1.printStackTrace();
							JOptionPane.showConfirmDialog(instance, "Unknown Host!\nCan't connect to this Address!");
						}
					} else {
						serversToStore[i] = Config.getInstance().servers[i];
					}
				}
				Config.getInstance().servers = serversToStore;
				Config.save();
				updateServerList();
			}
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
			dispose();
			loginFrame.setVisible(true);
			loginFrame.updateServerList();
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
}
