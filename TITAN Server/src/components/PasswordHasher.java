package components;

import java.security.MessageDigest;

public class PasswordHasher {

	public static String generateMD5(char[] md5) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] array = md.digest(DataUtils.charToBytes(md5));
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	public static String generateMD5(String md5) {
		return generateMD5(md5.toCharArray());
	}

	public static boolean validatePassword(char[] password, String string) {
		return generateMD5(password).equals(string) ? true : false;
	}
}
