package components;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

import main.Config;

import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.util.test.UncloseableOutputStream;

public class Encryption {

	private static final int secpar = 2048;
	private static final int certainty = 12;
	private static final int symetricAlgorithm = PGPEncryptedData.AES_256;

	public static byte[] decrypt(byte[] bytes) throws PGPException, IOException {
		if (bytes[0] == 0) {
			return bytes;
		}
		PGPObjectFactory pgpF = new PGPObjectFactory(bytes, new JcaKeyFingerprintCalculator());
		PGPEncryptedDataList encList = (PGPEncryptedDataList) pgpF.nextObject();
		PGPPublicKeyEncryptedData encP = (PGPPublicKeyEncryptedData) encList.get(0);
		InputStream decryptedData = encP.getDataStream(new BcPublicKeyDataDecryptorFactory(
				Config.getInstance().ClientKeyPair.getPrivateKey()));
		int ch;
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		while ((ch = decryptedData.read()) >= 0) {
			bOut.write(ch);
		}
		return bOut.toByteArray();
	}

	public static byte[] encrypt(byte[] bytes) throws IOException, PGPException {
		if (Config.getInstance().ServerKey == null) {
			throw new PGPException("No ServerKey!");
		}
		PGPDataEncryptorBuilder encBuilder = new BcPGPDataEncryptorBuilder(symetricAlgorithm)
				.setWithIntegrityPacket(true);
		PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encBuilder);
		encGen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(Config.getInstance().ServerKey));
		ByteArrayOutputStream baOut = new ByteArrayOutputStream();
		OutputStream cOut = encGen.open(new UncloseableOutputStream(baOut), bytes.length);
		cOut.write(bytes);
		cOut.close();
		return baOut.toByteArray();

	}

	public static byte[] keyToBytes(PGPPublicKey publicKey) throws IOException {
		return publicKey.getEncoded();
	}

	public static byte[] keyToBytes(PGPPrivateKey privateKey) throws IOException {
		return privateKey.getPrivateKeyDataPacket().getEncoded();
	}

	public static PGPPublicKey bytesToPublicKey(byte[] bytes) throws IOException, PGPException {
		PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(bytes, new JcaKeyFingerprintCalculator());
		Iterator<PGPPublicKeyRing> rIt = pgpPub.getKeyRings();

		while (rIt.hasNext()) {
			PGPPublicKeyRing kRing = rIt.next();
			Iterator<PGPPublicKey> kIt = kRing.getPublicKeys();

			while (kIt.hasNext()) {
				PGPPublicKey k = kIt.next();

				if (k.isEncryptionKey()) {
					return k;
				}
			}
		}
		throw new IllegalArgumentException("Can't find encryption key in key ring.");
	}

	public static PGPPrivateKey bytesToPrivateKey(byte[] bytes) throws IOException, PGPException {
		PGPSecretKeyRingCollection pgpPub = new PGPSecretKeyRingCollection(bytes, new JcaKeyFingerprintCalculator());
		Iterator<PGPSecretKeyRing> rIt = pgpPub.getKeyRings();

		while (rIt.hasNext()) {
			PGPSecretKeyRing kRing = rIt.next();
			Iterator<PGPSecretKey> kIt = kRing.getSecretKeys();

			while (kIt.hasNext()) {
				PGPSecretKey k = kIt.next();

				if (k.isMasterKey()) {
					PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(
							new BcPGPDigestCalculatorProvider()).build(new char[] {});
					return k.extractPrivateKey(decryptor);
				}
			}
		}
		throw new IllegalArgumentException("Can't find encryption key in key ring.");
	}

	public static void writeKey(PGPPublicKey key, String file) throws IOException {
		writeKey(key, new File(file));
	}

	public static void writeKey(PGPPublicKey key, File file) throws IOException {
		PGPPublicKeyRing ring = new PGPPublicKeyRing(key.getEncoded(), new JcaKeyFingerprintCalculator());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		ring.encode(out);
		out.close();
	}

	public static BcPGPKeyPair genRsaKeyRing() throws PGPException {
		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
		// RSA KeyGen parameters
		BigInteger publicExponent = BigInteger.valueOf(0x10001);
		RSAKeyGenerationParameters rsaKeyGenerationParameters = new RSAKeyGenerationParameters(publicExponent,
				new SecureRandom(), secpar, certainty);
		kpg.init(rsaKeyGenerationParameters);
		// generate master key (signing) and subkey (enc)
		Date now = new Date();
		return new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, kpg.generateKeyPair(), now);
	}

	public static PGPPublicKey readPublicKey(String file) throws FileNotFoundException, IOException, PGPException {
		return readPublicKey(new File(file));
	}

	public static PGPPublicKey readPublicKey(File file) throws FileNotFoundException, IOException, PGPException {
		return readPublicKey(new FileInputStream(file));
	}

	public static PGPPublicKey readPublicKey(InputStream in) throws IOException, PGPException {
		in = PGPUtil.getDecoderStream(in);

		PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new JcaKeyFingerprintCalculator());
		Iterator<PGPPublicKeyRing> rIt = pgpPub.getKeyRings();

		while (rIt.hasNext()) {
			PGPPublicKeyRing kRing = rIt.next();
			Iterator<PGPPublicKey> kIt = kRing.getPublicKeys();

			while (kIt.hasNext()) {
				PGPPublicKey k = kIt.next();

				if (k.isEncryptionKey()) {
					return k;
				}
			}
		}

		throw new IllegalArgumentException("Can't find encryption key in key ring.");
	}
}