package no.nav.skanmotreferansenr.pgpDecrypt.manuelltesting;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import static no.nav.skanmotreferansenr.pgpDecrypt.manuelltesting.PGPKeyUtil.readPublicKey;

// Metoder som er lite bearbeidet, men praktiske for testformål
@Slf4j
public class PGPEncryptUtil {

	public static void encryptFile(
			File inputFile,
			File outputFile,
			InputStream pubKey,
			boolean armor,
			boolean withIntegrityCheck)
			throws IOException, PGPException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
		PGPPublicKey encKey = readPublicKey(pubKey);
		encryptFile(out, inputFile, encKey, armor, withIntegrityCheck);
		out.close();
	}

	private static void encryptFile(
			OutputStream out,
			File inputFile,
			PGPPublicKey encKey,
			boolean armor,
			boolean withIntegrityCheck) throws IOException, PGPException {
		if (armor) {
			out = new ArmoredOutputStream(out);
		}

		try {
			PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));

			cPk.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("BC"));

			OutputStream cOut = cPk.open(out, new byte[1 << 16]);

			PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
					PGPCompressedData.ZIP);

			PGPUtil.writeFileToLiteralData(comData.open(cOut), PGPLiteralData.BINARY, inputFile, new byte[1 << 16]);

			comData.close();
			cOut.close();

			if (armor) {
				out.close();
			}
		} catch (PGPException e) {
			log.error(e.getMessage(), e);
			throw e;
		}
	}
}
