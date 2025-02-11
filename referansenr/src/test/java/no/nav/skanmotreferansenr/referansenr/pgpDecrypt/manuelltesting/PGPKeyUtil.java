package no.nav.skanmotreferansenr.referansenr.pgpDecrypt.manuelltesting;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

// Metoder som er lite bearbeidet, men praktiske for testformål
public class PGPKeyUtil {

	/**
	 * Search a secret key ring collection for a secret key corresponding to keyID if it
	 * exists.
	 *
	 * @param pgpSec a secret key ring collection.
	 * @param keyID  keyID we want.
	 * @param pass   passphrase to decrypt secret key with.
	 * @return the private key.
	 * @throws PGPException
	 */
	public static PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pgpSec, long keyID, char[] pass)
			throws PGPException {
		PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

		if (pgpSecKey == null) {
			return null;
		}

		return pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
	}

	/**
	 * A simple routine that opens a key ring file and loads the first available key
	 * suitable for encryption.
	 *
	 * @param input data stream containing the public key data
	 * @return the first public key found.
	 * @throws IOException
	 * @throws PGPException
	 */
	public static PGPPublicKey readPublicKey(InputStream input) throws IOException, PGPException {
		PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
				PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

		// we just loop through the collection till we find a key suitable for encryption, in the real
		// world you would probably want to be a bit smarter about this.

		Iterator<PGPPublicKeyRing> keyRingIter = pgpPub.getKeyRings();
		while (keyRingIter.hasNext()) {

			PGPPublicKeyRing keyRing = keyRingIter.next();

			Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
			while (keyIter.hasNext()) {
				PGPPublicKey key = keyIter.next();

				if (key.isEncryptionKey()) {
					return key;
				}
			}
		}

		throw new IllegalArgumentException("Can't find encryption key in key ring.");
	}

	public static PGPSecretKey readSecretKey(String fileName) throws IOException, PGPException {
		InputStream keyIn = new BufferedInputStream(new FileInputStream(fileName));
		PGPSecretKey secKey = readSecretKey(keyIn);
		keyIn.close();
		return secKey;
	}

	/**
	 * A simple routine that opens a key ring file and loads the first available key
	 * suitable for signature generation.
	 *
	 * @param input stream to read the secret key ring collection from.
	 * @return a secret key.
	 * @throws IOException  on a problem with using the input stream.
	 * @throws PGPException if there is an issue parsing the input stream.
	 */
	public static PGPSecretKey readSecretKey(InputStream input) throws IOException, PGPException {
		PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
				PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

		//
		// we just loop through the collection till we find a key suitable for encryption, in the real
		// world you would probably want to be a bit smarter about this.
		//

		Iterator<PGPSecretKeyRing> keyRingIter = pgpSec.getKeyRings();
		while (keyRingIter.hasNext()) {
			PGPSecretKeyRing keyRing = keyRingIter.next();

			Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
			while (keyIter.hasNext()) {
				PGPSecretKey key = keyIter.next();

				if (key.isSigningKey()) {
					return key;
				}
			}
		}

		throw new IllegalArgumentException("Can't find signing key in key ring.");
	}
}