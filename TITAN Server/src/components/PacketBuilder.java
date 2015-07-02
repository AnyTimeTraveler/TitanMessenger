package components;

import java.io.IOException;
import java.util.zip.DataFormatException;

import main.Worker;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.javatuples.Pair;

import com.sun.xml.internal.ws.util.ByteArrayBuffer;

public class PacketBuilder {
	public static final byte[] SuccessPacket = new byte[] { (byte) 1 };
	public static final byte[] FailPacket = new byte[] { (byte) 0 };

	public static byte[] serialize(PGPPublicKey key, int type, byte[]... arrays) throws IOException, PGPException {
		return serialize(true, key, type, arrays);
	}

	public static byte[] serialize(int type, byte[]... arrays) throws IOException, PGPException {
		return serialize(false, null, type, arrays);
	}

	private static byte[] serialize(boolean doEncrypt, PGPPublicKey key, int type, byte[]... arrays)
			throws IOException, PGPException {
		ByteArrayBuffer buffer = new ByteArrayBuffer();
		buffer.write(DataUtils.intToByteArray(type));
		buffer.write(DataUtils.intToByteArray(arrays.length));
		for (byte[] array : arrays) {
			buffer.write(DataUtils.intToByteArray(array.length));
		}
		for (byte[] array : arrays) {
			buffer.write(array);
		}

		byte[] data = buffer.getRawData();
		buffer.close();

		if (doEncrypt) {
			data = Encryption.encrypt(data, key);
		}

		byte[] output = new byte[data.length + 1];
		output[0] = doEncrypt ? (byte) 1 : (byte) 0;
		System.arraycopy(data, 0, output, 1, data.length);

		return DataUtils.compress(output);
	}

	public static Pair<Integer, byte[][]> unseriealize(byte[] input) throws IOException, PGPException,
			DataFormatException {

		// Decompress it
		input = DataUtils.decompress(input);

		if (!(input[0] == (byte) 1 || input[0] == (byte) 0)) {
			throw new DataFormatException("Can't process Data! Check encryption.");
		}
		boolean isEncrypted = input[0] == (byte) 1 ? true : false;

		byte[] workingData = new byte[input.length - 1];

		System.arraycopy(input, 1, workingData, 0, input.length - 1);

		if (isEncrypted) {
			// Decrypt it
			workingData = Encryption.decrypt(workingData, Worker.serverKeyPair.getPrivateKey());
		}

		byte[] temp = new byte[4];
		int pos = 0;
		System.arraycopy(workingData, pos, temp, 0, 4);
		pos += 4;
		int type = DataUtils.byteArrayToInt(temp);

		temp = new byte[4];
		System.arraycopy(workingData, pos, temp, 0, 4);
		pos += 4;
		int amount = DataUtils.byteArrayToInt(temp);

		int[] lengths = new int[amount];
		for (int i = 0; i < amount; i++) {
			temp = new byte[4];
			System.arraycopy(workingData, pos, temp, 0, 4);
			pos += 4;
			lengths[i] = DataUtils.byteArrayToInt(temp);
		}

		byte[][] output;
		output = new byte[amount][];
		for (int i = 0; i < amount; i++) {

			ByteArrayBuffer buffer = new ByteArrayBuffer();
			for (int k = 0; k < lengths[i]; k++) {
				buffer.write(workingData[pos]);
				pos++;
			}
			output[i] = new byte[lengths[i]];
			System.arraycopy(buffer.getRawData(), 0, output[i], 0, lengths[i]);
			buffer.reset();
			buffer.close();
		}
		return Pair.with(type, output);

	}
}
