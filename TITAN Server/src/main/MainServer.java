package main;

import frame.LogFrame;
import io.NioUnit;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import components.DBConnector;

public class MainServer {

	private static Worker	worker;
	private static NioUnit	ioServer;
	private static Object	frameWaiter;
	private static boolean	doGUI;
	private static Integer	hostPort;

	public static void main(String[] args) {

		if (args.length == 2) {
			hostPort = Integer.valueOf(args[0]);
			doGUI = Boolean.valueOf(args[1]);
		} else {
			doGUI = true;
			hostPort = 9000;
		}
		String configFilePath = null;
		frameWaiter = new Object();
		try {
			configFilePath = MainServer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() +
					"config.cfg";
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (doGUI) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (	ClassNotFoundException |
						InstantiationException |
						IllegalAccessException |
						UnsupportedLookAndFeelException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Thread frameThread = new Thread(new Runnable() {

				public void run() {
					try {
						new LogFrame();
						synchronized (frameWaiter) {
							frameWaiter.notify();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			frameThread.setName("LogFrame");
			frameThread.setDaemon(true);
			frameThread.start();

			synchronized (frameWaiter) {
				try {
					frameWaiter.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		LogFrame.updateLog("Loading config...");
		Config.load(configFilePath);

		new DBConnector();
		LogFrame.updateLog("Reserving " + Config.getInstance().Slots + " slots...");
		new ClientManagement();
		worker = new Worker();
		Thread workerThread = new Thread(worker);
		workerThread.setName("Worker");
		workerThread.setDaemon(false);
		try {
			ioServer = new NioUnit(hostPort, worker);
		} catch (IOException e) {
			System.err.println("Failed to bind Socket! Maybe there is another instance already running?");
			e.printStackTrace();
			System.exit(-1);
		}
		Thread ioServerThread = new Thread(ioServer);
		ioServerThread.setName("NioServer");
		ioServerThread.setDaemon(true);
		LogFrame.updateLog("Starting NioServer...");
		ioServerThread.start();
		LogFrame.updateLog("Server started!");
		LogFrame.updateLog("Starting BackgroundWorker...");
		workerThread.start();
		LogFrame.updateLog("Worker started!");
	}

	public static Worker getWorker() {
		return worker;
	}

	public static NioUnit getIoServer() {
		return ioServer;
	}
}
