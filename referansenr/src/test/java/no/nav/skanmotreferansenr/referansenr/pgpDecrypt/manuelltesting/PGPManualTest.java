package no.nav.skanmotreferansenr.referansenr.pgpDecrypt.manuelltesting;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static no.nav.skanmotreferansenr.referansenr.pgpDecrypt.manuelltesting.PGPDecryptUtil.decryptFile;
import static org.junit.jupiter.api.Assertions.fail;

public class PGPManualTest {

	@BeforeAll
	static void beforeAll() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Disabled("Praktisk ved manuell testing/prøving og feiling")
	@Test
	public void testEncryptionAndDecryption() throws Exception {
		// encrypted.zip.pgp er den PGP-krypterte mappen som kommer fra IM
		FileInputStream encryptedZipAsStream = getEncryptedFilesFromIM();
		InputStream decryptedInputStream = decryptFile(encryptedZipAsStream, getPrivateKey(), "Test321".toCharArray()); // "Test321" for RSA. "Test123" for ElGamal

		// Den dekrypterte zippede folderen som blir brukt videre i Camel-ruten
		File targetFile = new File("src/test/resources/test/decryptResult.zip");
		FileUtils.copyInputStreamToFile(decryptedInputStream, targetFile);
	}

	@Disabled
	@Test
	void shouldDecryptFile() throws IOException, NoSuchProviderException, PGPException {
		// encrypted.zip.pgp er den PGP-krypterte mappen som kommer fra IM
		//FileInputStream encryptedZipAsStream = getEncryptedFilesFromIM();
		FileInputStream encryptedZipAsStream = new FileInputStream("src/test/resources/OVRIG-20200529-2.zip.pgp");

		InputStream decryptedInputStream = decryptFile(encryptedZipAsStream, getPrivateKey(), "Test321".toCharArray()); // "Test321" for RSA. "Test123" for ElGamal

		// Den dekrypterte zippede folderen som blir brukt videre i Camel-ruten
		File targetFile = new File("src/test/resources/test/decryptResult.zip");
		FileUtils.copyInputStreamToFile(decryptedInputStream, targetFile);

		// Sjekk at output inneholder alle filer og at xml-teksten er lesbar
		List<String> listOfFileNames = Arrays.asList("OVRIG.20200529-2-1.pdf", "OVRIG.20200529-2-1.xml", "OVRIG.20200529-2-2.pdf",
				"OVRIG.20200529-2-2.xml", "OVRIG.20200529-2-3.pdf", "OVRIG.20200529-2-3.xml", "OVRIG.20200529-2-4.xml",
				"OVRIG.20200529-2-6.pdf", "OVRIG.20200529-2-5.pdf", "OVRIG.20200529-2-6.xml"
		);
		ZipFile zipFile = new ZipFile("src/test/resources/test/decryptResult.zip");

		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (!listOfFileNames.contains(entry.getName())) {
				fail("Fikk uforventet filnavn: " + entry.getName());
			}
			InputStream stream = zipFile.getInputStream(entry);
			if (entry.getName().contains("xml")) {
				String result = IOUtils.toString(stream, StandardCharsets.UTF_8);
				if (!result.contains("journalpost")) {
					fail("Fil " + entry.getName() + " mangler tekst i xml");
				}
			}
		}
	}

	@Test
	@Disabled
	public void generateEncryptedFile() throws Exception {
		// encrypted.zip.pgp er den PGP-krypterte mappen som kommer fra IM
		getEncryptedFilesFromIM();
	}

	private FileInputStream getEncryptedFilesFromIM() throws IOException, PGPException {
		String filToEncrypt = "09.06.2020_R123456783_3_4000";
		// På Iron Mountain sin side
		File skannedeDokumenterZip = new File("src/test/resources/"+filToEncrypt+".zip");
		String filnavnKryptertZip = "src/test/resources/"+filToEncrypt+".zip.pgp";
		File outputfil = new File(filnavnKryptertZip);

		// Skriv krypterte data til spesifisert outputfil, og returner som FileInputStream
		PGPEncryptUtil.encryptFile(skannedeDokumenterZip, outputfil, getPublicKey(), false, false);

		return new FileInputStream(filnavnKryptertZip);
	}

	private BufferedInputStream getPublicKey() throws IOException {
		FileInputStream publicKeyStream = new FileInputStream("src/test/resources/pgp/publicKeyElGamal.gpg");

		return new BufferedInputStream(publicKeyStream);
	}

	private BufferedInputStream getPrivateKey() throws FileNotFoundException {
		FileInputStream privateKeyStream = new FileInputStream("src/test/resources/pgp/privateKeyRSA.gpg");

		return new BufferedInputStream(privateKeyStream);
	}
}
