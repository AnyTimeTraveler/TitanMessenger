package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import objects.Server;
import objects.User;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {

	public PGPPublicKey	ServerKey;
	public BcPGPKeyPair	ClientKeyPair;
	public String		Language;
	public User			thisUser;
	public String[]		users;
	public Server[]		servers;

	public Config() {
		this.Language = "en"; // ENGLISCH
		this.thisUser = null;
		this.users = new String[0];
		try {
			this.servers = new Server[] { new Server("Localhost", "localhost", 9000),
					new Server("Amazon", "52.28.63.188", 9000) };
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.ClientKeyPair = null;
		this.ServerKey = null;
	}

	// DON'T TOUCH THE FOLLOWING CODE
	private static Config	instance;
	private static File		location;

	public static Config getInstance() {
		if (instance == null) {
			instance = fromDefaults();
		}
		return instance;
	}

	public static void load(File file) {
		instance = fromFile(file);

		// no config file found
		if (instance == null) {
			instance = fromDefaults();
		} else {
			location = file;
		}
	}

	public static boolean save() {
		if (instance == null || location == null) {
			return false;
		}
		Config.getInstance().toFile(location);
		return true;
	}

	public static boolean load() {
		if (instance == null || location == null) {
			return false;
		}
		instance = Config.fromFile(location);
		return true;
	}

	public static void load(String file) {
		load(new File(file));
	}

	private static Config fromDefaults() {
		Config config = new Config();
		return config;
	}

	public void toFile(String file) {
		toFile(new File(file));
	}

	public void toFile(File file) {

		try {
			file.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonConfig = gson.toJson(this);
		FileWriter writer;
		try {
			writer = new FileWriter(file);
			writer.write(jsonConfig);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Config fromFile(File configFile) {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
			return gson.fromJson(reader, Config.class);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}
}