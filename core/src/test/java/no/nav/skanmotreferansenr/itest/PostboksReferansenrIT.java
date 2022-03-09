package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class PostboksReferansenrIT extends AbstractItest {

	public static final String INNGAAENDE = "inngaaende";
	public static final String FEILMAPPE = "feilmappe";

	private final String ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456789_1_1000";
	private final String ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION = "09.06.2020_R100000000_1_1000_ordered_xml_first_big";

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
	public void shouldBehandleZip() throws IOException {
		// 09.06.2020_R123456789_1_1000.zip
		// OK   - 09.06.2020_R123456789_0001
		// OK   - 09.06.2020_R123456789_0002 (mangler førstesidemetadata, Oppretter journalpost med tema UKJ)
		// FEIL - 09.06.2020_R123456789_0003 (valideringsfeil, mangler referansenr)
		// FEIL - 09.06.2020_R123456789_0004 (mangler xml)
		// FEIL - 09.06.2020_R123456789_0005 (mangler pdf)
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip");

		await().atMost(ofSeconds(15)).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
						.resolve(ZIP_FILE_NAME_NO_EXTENSION))
						.collect(Collectors.toList())).hasSize(3);
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"09.06.2020_R123456789_0003.zip",
				"09.06.2020_R123456789_0004.zip",
				"09.06.2020_R123456789_0005.zip");
		verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1)));
		verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND)));
		verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
		verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));

	}

	@Test
	public void shouldBehandleZipXmlOrderedLastWithinCompletionTimeout() throws IOException {
		// 09.06.2020_R100000000_1_1000_ordered_xml_first_big.zip
		// OK   - 09.06.2020_R100000000_0001
		// OK   - 09.06.2020_R100000000_0002 (mangler førstesidemetadata, Oppretter journalpost med tema UKJ)
		// FEIL - 09.06.2020_R100000000_0003 (valideringsfeil, mangler referansenr)
		// FEIL - 09.06.2020_R100000000_0004 (mangler xml)
		// FEIL - 09.06.2020_R100000000_0005 (mangler pdf)
		// OK   - 09.06.2020_R100000000_0006
		// ...
		// OK   - 09.06.2020_R100000000_0059
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION + ".zip");

		await().atMost(ofSeconds(20)).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
						.resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION))
						.collect(Collectors.toList())).hasSize(3);
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_ORDERED_XML_FIRST_NO_EXTENSION))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"09.06.2020_R100000000_0003.zip",
				"09.06.2020_R100000000_0004.zip",
				"09.06.2020_R100000000_0005.zip");
		verify(exactly(55), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1)));
		verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND)));
		verify(exactly(56), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
		verify(exactly(110), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));
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