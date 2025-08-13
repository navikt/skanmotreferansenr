package no.nav.skanmotreferansenr.itest;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AvstemRouteIT extends AbstractItest {

	private static final String AVSTEMMINGSFILMAPPE = "avstemmappe";
	private static final String PROCESSED = "processed";
	private static final String AVSTEMMINGSFIL = "04.01.2024_avstemmingsfil_1.txt";

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		super.setUpStubs();
		final Path avstem = sshdPath.resolve(AVSTEMMINGSFILMAPPE);
		final Path processed = avstem.resolve(PROCESSED);
		preparePath(avstem);
		preparePath(processed);
	}

	@Test
	public void shouldOpprettJiraOppgaveForFeilendeAvstemreferanser() throws IOException {
		stubJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");

		copyFileFromClasspathToAvstem(AVSTEMMINGSFIL);

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);
		assertThat(Files.exists(filePath)).isTrue();
		assertAntallProsesserteFiler(0);

		await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertAntallProsesserteFiler(1);
					verifyRequest();
				});

		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED))) {
			List<String> processedMappe = files.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
					.toList();
			assertThat(processedMappe).containsExactly(AVSTEMMINGSFIL);
		}

	}

	@Test
	public void shouldNotOpprettJiraWhenFeilendeAvstemReferanserIsNull() throws IOException {
		stubPostAvstemJournalpost("journalpostapi/null-avstem.json");

		copyFileFromClasspathToAvstem(AVSTEMMINGSFIL);

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);

		assertThat(Files.exists(filePath)).isTrue();
		assertAntallProsesserteFiler(0);

		await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertAntallProsesserteFiler(1);
					verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
				});

	}

	@Test
	public void shouldNotProcessAvstemmingsFileWhenJiraThrowException() throws IOException {
		stubBadRequestJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");


		copyFileFromClasspathToAvstem(AVSTEMMINGSFIL);

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);
		assertThat(Files.exists(filePath)).isTrue();
		assertAntallProsesserteFiler(0);

		await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertAntallProsesserteFiler(0);
					assertAntallUbehandletFiler(2);
				});
	}

	@Test
	public void shouldOpprettJiraOppgaveWhenAvstemmingsfilIsMissing() {
		stubBadRequestJiraOpprettOppgave();

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);
		assertThat(Files.exists(filePath)).isFalse();
		assertAntallProsesserteFiler(0);

		await()
				.atMost(ofSeconds(20))
				.untilAsserted(() -> {
					assertAntallProsesserteFiler(0);
					verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
					verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
				});
	}

	private void verifyRequest() {
		verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
		verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
		verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
	}

	private void copyFileFromClasspathToAvstem(final String txtFilename) throws IOException {
		Files.copy(new ClassPathResource(txtFilename).getInputStream(), sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(txtFilename));
	}

	private void preparePath(Path path) {
		try {
			if (!Files.exists(path)) {
				Files.createDirectory(path);
			} else {
				FileUtils.cleanDirectory(path.toFile());
			}
		} catch (IOException e) {

		}
	}

	@SneakyThrows
	private void assertAntallProsesserteFiler(int forventetAntallFiler) {
		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED))) {
			assertThat(files.collect(Collectors.toSet())).hasSize(forventetAntallFiler);
		}
	}

	@SneakyThrows
	private void assertAntallUbehandletFiler(int forventetAntallFiler) {
		try (Stream<Path> files = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE))) {
			assertThat(files.collect(Collectors.toSet())).hasSize(forventetAntallFiler);
		}
	}
}
