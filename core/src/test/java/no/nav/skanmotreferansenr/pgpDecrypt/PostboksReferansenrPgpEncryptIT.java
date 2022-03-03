package no.nav.skanmotreferansenr.pgpDecrypt;

import no.nav.skanmotreferansenr.itest.AbstractItest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import wiremock.org.apache.commons.io.FileUtils;
import wiremock.org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PostboksReferansenrPgpEncryptIT extends AbstractItest {

	public static final String INNGAAENDE = "inngaaende";
	public static final String FEILMAPPE = "feilmappe";

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		super.setUpStubs();
		final Path inngaaende = sshdPath.resolve(INNGAAENDE);
		final Path processed = inngaaende.resolve("processed");
		final Path feilmappe = sshdPath.resolve(FEILMAPPE);
		try {
			preparePath(inngaaende);
			preparePath(processed);
			preparePath(feilmappe);
		} catch (Exception e) {
			//noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
		}
	}

	@Test
	public void shouldBehandlePgpEncryptedZip() throws IOException {
		// 09.06.2020_R123456780_1_2000.zip
		// OK   - 09.06.2020_R123456780_0001
		// OK   - 09.06.2020_R123456780_0002 (mangler førstesidemetadata, Oppretter journalpost med tema UKJ)
		// FEIL - 09.06.2020_R123456780_0003 (valideringsfeil, mangler referansenr)
		// FEIL - 09.06.2020_R123456780_0004 (mangler xml)
		// FEIL - 09.06.2020_R123456780_0005 (mangler pdf)

		final String ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456780_1_2000";
		copyFileFromClasspathToInngaaende(ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		await().atMost(ofSeconds(15)).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
						.resolve(ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION))
						.collect(Collectors.toList())).hasSize(3);
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"09.06.2020_R123456780_0003.zip",
				"09.06.2020_R123456780_0004.zip",
				"09.06.2020_R123456780_0005.zip");
		verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1)));
		verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND)));
		verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
		verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));
	}

	@Test
	public void shouldFailWhenPrivateKeyDoesNotMatchPublicKey() throws IOException {
		// 09.06.2020_R123456783_3_1000.zip.pgp er kryptert med publicKeyElGamal (i stedet for publicKeyRSA)
		// Korresponderende RSA-private key vil da feile i forsøket på dekryptering

		final String ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456783_3_1000";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		assertTrue(Files.exists(sshdPath.resolve(INNGAAENDE).resolve(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp")));

		await().atMost(15, SECONDS).untilAsserted(() -> {
			assertTrue(Files.exists(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp")));
		});
	}

	private void preparePath(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		} else {
			FileUtils.cleanDirectory(path.toFile());
		}
	}

	private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
		Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
	}
}