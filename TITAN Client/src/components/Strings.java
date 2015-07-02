package components;

import main.Config;

public abstract class Strings {

	public static String Login;
	public static String ApplicationName = "TITAN Messenger";
	public static String Register;
	public static String Username;
	public static String FirstName;
	public static String LastName;
	public static String Birthday;
	public static String Address;
	public static String PhoneNumber;
	public static String EMail = "E-Mail";

	public Strings() {
		if (Config.getInstance().Language.equals("de")) {

			Strings.Login = "Einloggen";
			Strings.Register = "Registrieren";
			Strings.Username = "Benutzername";
			Strings.FirstName = "Vorname";
			Strings.LastName = "Nachname";
			Strings.Birthday = "Geburtstag";
			Strings.Address = "Adresse";
			Strings.PhoneNumber = "Telefonnummer";

		} else if (Config.getInstance().Language.equals("en")) {

			Strings.Login = "Login";
			Strings.Register = "Register";
			Strings.FirstName = "First Name";
			Strings.Address = "Last Name";
			Strings.PhoneNumber = "Phone Number";

		} else {

			System.err.println("Falsche Sprache!");

		}
	}

	public static String getString(String string) {
		if (string.equals("Login")) {
			return Login;
		}
		return null;
	}
}
