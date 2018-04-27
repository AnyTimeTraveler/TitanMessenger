package main;

import io.NioUnit;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import visual.LoginFrame;

import components.DBConnector;
import components.DataHandler;

public class MainClient {

	public static NioUnit		ioUnit;
	public static DataHandler	dataHandler;

	public static void main(String[] args) {

		Config.load("/home/xxy/TITAN.cfg");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (	ClassNotFoundException |
					InstantiationException |
					IllegalAccessException |
					UnsupportedLookAndFeelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
