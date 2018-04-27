package visual;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import main.Config;
import main.MainClient;
import objects.Server;

import org.bouncycastle.openpgp.PGPException;

import components.DataHandler;
import components.Encryption;

public class LoginFrame extends JFrame {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6339208158208748227L;
	private static LoginFrame	instance;
	private Observer			loginObserver;

	private JPanel				contentPane;
	private JTextField			tfUsername;
	private JPasswordField		pfPassword;
	private JComboBox<String>	cbServer;
	private JButton				btnLogin;
	private JButton				btnCancel;
	public static Object		waiter;
	private JLabel				lblError;
	private JPanel				loginPanel;
	private JPanel				animationPanel;
	private JLabel				lblStatus;

	private boolean				runningLoginProcess;

	/**
	 * Create the frame.
	 */
	public LoginFrame() {
		super("TITAN Messenger Login");
		waiter = new Object();
		instance = this;
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize();
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout());
		setContentPane(contentPane);

		loginPanel = new JPanel();

		animationPanel = new JPanel();
		lblStatus = new JLabel("Loading Frame...");
		JLabel loadingAnimation = new JLabel();
		loadingAnimation = new JLabel(new ImageIcon(MainClient.class.getResource("/images/loading_spinner.gif")));
		animationPanel.setLayout(new BorderLayout(10, 10));
		loadingAnimation.setLayout(new BorderLayout());
		loadingAnimation.add(lblStatus, BorderLayout.NORTH);
		animationPanel.add(loadingAnimation, BorderLayout.CENTER);
		JLabel lblUsername = new JLabel("Username");

		JLabel lblPassword = new JLabel("Password");

		pfPassword = new JPasswordField();
		lblPassword.setLabelFor(pfPassword);
		MyActionListener actionListener = new MyActionListener();

		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(actionListener);
		btnLogin = new JButton("Login");
		btnLogin.addActionListener(actionListener);

		cbServer = new JComboBox<String>();
		cbServer.addItemListener(new MyItemListener());

		updateServerList();

		JLabel lblServer = new JLabel("Server");
		lblServer.setLabelFor(cbServer);

		lblError = new JLabel();
		lblError.setVisible(false);

		tfUsername = new JTextField();
		lblUsername.setLabelFor(tfUsername);
		tfUsername.setColumns(10);
		if (Config.getInstance().thisUser != null) {
			tfUsername.setText(Config.getInstance().thisUser.getUsername());
		}

		GroupLayout gl_loginPanel = new GroupLayout(loginPanel);
		gl_loginPanel.setHorizontalGroup(gl_loginPanel.createParallelGroup(Alignment.LEADING).addGroup(gl_loginPanel
				.createSequentialGroup()
				.addGroup(gl_loginPanel
						.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_loginPanel.createSequentialGroup().addGap(36)
								.addComponent(lblError, GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE))
						.addGroup(gl_loginPanel
								.createSequentialGroup()
								.addGap(76)
								.addGroup(gl_loginPanel
										.createParallelGroup(Alignment.LEADING, false)
										.addComponent(tfUsername)
										.addGroup(gl_loginPanel
												.createSequentialGroup()
												.addComponent(btnCancel,
													GroupLayout.PREFERRED_SIZE,
													82,
													GroupLayout.PREFERRED_SIZE)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(btnLogin,
													GroupLayout.DEFAULT_SIZE,
													GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)).addComponent(lblPassword)
										.addComponent(lblUsername).addComponent(pfPassword)
										.addComponent(cbServer, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(lblServer)))).addGap(32)));
		gl_loginPanel.setVerticalGroup(gl_loginPanel.createParallelGroup(Alignment.TRAILING).addGroup(gl_loginPanel
				.createSequentialGroup()
				.addContainerGap()
				.addComponent(lblUsername)
				.addGap(5)
				.addComponent(tfUsername,
					GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.addGap(18)
				.addComponent(lblPassword)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(pfPassword,
					GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.addGap(10)
				.addComponent(lblServer)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(cbServer,
					GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)
				.addGap(18)
				.addGroup(gl_loginPanel.createParallelGroup(Alignment.BASELINE).addComponent(btnCancel)
						.addComponent(btnLogin)).addPreferredGap(ComponentPlacement.RELATED).addComponent(lblError)
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		loginPanel.setLayout(gl_loginPanel);
		contentPane.add(loginPanel, BorderLayout.CENTER);
		addWindowListener(new MyWindowListener());
		setVisible(true);
	}

	private void setSize() {
		setBounds(100, 100, 350, 292);
	}

	/**
	 * 
	 */
	public void setLoginText(String text) {
		if (runningLoginProcess) {
			instance.lblStatus.setText(text);
		}
	}

	public void updateServerList() {
		cbServer.removeAllItems();
		for (Server server : Config.getInstance().servers) {
			cbServer.addItem(server.getName());
		}
		cbServer.addItem("Edit Servers...");
	}

	private void login() {
		Thread loginWorker = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					runningLoginProcess = true;
					loginObserver = new LoginObserver();

					instance.contentPane.removeAll();
					instance.contentPane.add(animationPanel, BorderLayout.CENTER);
					instance.pack();
					instance.repaint();

					instance.lblStatus.setText("Generating Keypair...");
					Config.getInstance().ClientKeyPair = Encryption.genRsaKeyRing();

					DataHandler.login(Server.getByName((String) instance.cbServer.getSelectedItem()),
						instance.tfUsername.getText(),
						instance.pfPassword.getPassword(),
						loginObserver);
					while (!DataHandler.isLoginComplete()) {
						Thread.sleep(100);
					}

					if (DataHandler.isLoggedIn()) {
						instance.lblStatus.setText("Login sucessfull!");
						Config.save();
						EventQueue.invokeLater(new Runnable() {

							@Override
							public void run() {
								new MessengerFrame();
							}
						});
						instance.dispose();
					} else {
						instance.contentPane.removeAll();
						instance.contentPane.add(loginPanel, BorderLayout.CENTER);
						instance.setSize();
						instance.repaint();
						instance.lblError.setIcon(new ImageIcon(LoginFrame.class
								.getResource("/javax/swing/plaf/metal/icons/ocean/error.png")));
						instance.lblError.setText("Error: Login failed!");
						instance.lblError.setVisible(true);
						Config.load();
					}
					runningLoginProcess = false;
				} catch (IOException | PGPException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					runningLoginProcess = false;
				}
			}
		});
		loginWorker.setName("LoginWorker");
		loginWorker.setDaemon(true);
		loginWorker.start();
	}

	private class LoginObserver implements Observer {

		@Override
		public void update(Observable arg0, Object arg1) {
			if (arg1 instanceof String) {
				instance.lblStatus.setText((String) arg1);
			}
		}

	}

	private class MyWindowListener implements WindowListener {

		@Override
		public void windowOpened(WindowEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowClosing(WindowEvent e) {
			instance.dispose();
		}

		@Override
		public void windowClosed(WindowEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowIconified(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeiconified(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowActivated(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			// TODO Auto-generated method stub

		}
	}

	private class MyItemListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent event) {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				String item = (String) event.getItem();
				if (item.equals("Edit Servers...")) {
					instance.setVisible(false);
					// Open the ServersFrame
					EventQueue.invokeLater(new Runnable() {

						@Override
						public void run() {
							new ServerFrame(instance);
						}
					});
				}
			}
		}
	}

	private class MyActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(btnLogin)) {
				login();
			} else if (e.getSource().equals(btnCancel)) {
				instance.dispose();
				System.exit(0);
			}
		}
	}
}
