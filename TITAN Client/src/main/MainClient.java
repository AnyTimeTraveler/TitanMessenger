package main;

import frames.LoginFrame;
import io.NioUnit;

import java.awt.EventQueue;
import java.io.IOException;

import components.DBConnector;
import components.DataHandler;

public class MainClient {

	public static NioUnit ioUnit;
	public static DataHandler dataHandler;

	public static void main(String[] args) {

		Config.load("/home/xxy/TITAN.cfg");
		new DBConnector();
		dataHandler = new DataHandler();
		try {
			ioUnit = new NioUnit(dataHandler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		dataHandler.setIOUnit(ioUnit);

		Thread clientThread = new Thread(ioUnit);
		clientThread.setDaemon(true);
		clientThread.setName("NioClient");
		clientThread.start();

		Thread dataThread = new Thread(dataHandler);
		dataThread.setDaemon(true);
		dataThread.setName("DataHandler");
		dataThread.start();

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new LoginFrame();
			}
		});
	}
}
