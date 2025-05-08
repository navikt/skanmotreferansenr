package no.nav.skanmotreferansenr.referansenr.pgpDecrypt.manuelltesting;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static no.nav.skanmotreferansenr.referansenr.pgpDecrypt.manuelltesting.PGPKeyUtil.findSecretKey;

// Metoder som er lite bearbeidet, men praktiske for testformål
@Slf4j
public class PGPDecryptUtil {

	/**
	 * Decrypt the passed in message stream
	 *
	 * @return InputStream
	 */
	public static InputStream decryptFile(
			InputStream encryptedDataStream,
			InputStream privateKeyStream,
			char[] passwd)
			throws IOException, PGPException {
		InputStream in = PGPUtil.getDecoderStream(encryptedDataStream);

		try (privateKeyStream) {
			PGPEncryptedDataList enc = getPgpEncryptedData(in);
			InputStream clear = findPrivateKeyAndDecrypt(privateKeyStream, passwd, enc);

			JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clear);
			PGPCompressedData cData = (PGPCompressedData) plainFact.nextObject();
			InputStream compressedStream = new BufferedInputStream(cData.getDataStream());
			JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(compressedStream);
			Object message = pgpFact.nextObject();

			if (message instanceof PGPLiteralData literalDataMessage) {
				return literalDataMessage.getInputStream();
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("PGP-kryptert melding inneholder en signert melding - ingen faktiske data.");
			} else {
				throw new PGPException("PGP-kryptert melding har en ukjent datatype.");
			}
		} catch (PGPException e) {
			log.error("DecryptFile feilet med melding: {}", e.getMessage(), e);
			throw e;
		}
	}

	private static InputStream findPrivateKeyAndDecrypt(InputStream privateKeyStream, char[] passwd, PGPEncryptedDataList encryptedDataList) throws IOException, PGPException {
		// Find secret key (private key)
		PGPPrivateKey pgpPrivateKey = null;
		PGPPublicKeyEncryptedData publicKeyEncryptedData = null;
		PGPSecretKeyRingCollection pgpKeyRing = new PGPSecretKeyRingCollection(
				PGPUtil.getDecoderStream(privateKeyStream),
				new JcaKeyFingerprintCalculator()
		);

		Iterator<PGPEncryptedData> it = encryptedDataList.getEncryptedDataObjects();
		while (pgpPrivateKey == null && it.hasNext()) {
			publicKeyEncryptedData = (PGPPublicKeyEncryptedData) it.next();

			pgpPrivateKey = findSecretKey(pgpKeyRing, publicKeyEncryptedData.getKeyIdentifier().getKeyId(), passwd);
		}

		if (pgpPrivateKey == null) {
			throw new IllegalArgumentException("Gyldig privatnøkkel for melding ble ikke funnet.");
		}

		return publicKeyEncryptedData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pgpPrivateKey));
	}

	private static PGPEncryptedDataList getPgpEncryptedData(InputStream in) throws IOException {
		JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(in);

		// The first object might be a PGP marker packet.
		Object nextObjectInStream = pgpFactory.nextObject();
		if (nextObjectInStream instanceof PGPEncryptedDataList) {
			return (PGPEncryptedDataList) nextObjectInStream;
		} else {
			return (PGPEncryptedDataList) pgpFactory.nextObject();
		}
	}
}
