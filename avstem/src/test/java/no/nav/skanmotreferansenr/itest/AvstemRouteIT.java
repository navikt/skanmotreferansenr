package no.nav.skanmotreferansenr.itest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

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
		try {
			preparePath(avstem);
			preparePath(processed);
		} catch (Exception e) {
			// noop
		}
	}

	@Test
	public void shouldOpprettJiraOppgaveForFeilendeAvstemreferanser() throws IOException {
		stubJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");


		copyFileFromClasspathToAvstem(AVSTEMMINGSFIL);

		assertThat(Files.exists(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL))).isTrue();
		assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);

		Awaitility.await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(1);
					verifyRequest();
				});

		List<String> processedMappe = Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());

		assertThat(processedMappe).containsExactly(AVSTEMMINGSFIL);
	}

	@Test
	public void shouldNotOpprettJiraWhenFeilendeAvstemReferanserIsNull() throws IOException {
		stubPostAvstemJournalpost("journalpostapi/null-avstem.json");

		copyFileFromClasspathToAvstem(AVSTEMMINGSFIL);

		assertThat(Files.exists(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL))).isTrue();
		assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);

		Awaitility.await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(1);
					verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
				});

	}

	@Test
	public void shouldThrowExceptionJiraOppgaveForFeilendeAvstemReferanser() throws IOException {
		stubBadRequestJiraOpprettOppgave();
		stubPostAvstemJournalpost("journalpostapi/avstem.json");


		copyFileFromClasspathToAvstem(AVSTEMMINGSFIL);

		assertThat(Files.exists(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL))).isTrue();
		assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);

		Awaitility.await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);
					assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE)).collect(Collectors.toSet())).hasSize(2);
				});
	}

	@Test
	public void shouldOpprettJiraOppgaveWhenAvstemmingsfilIsMissing() throws IOException {
		stubBadRequestJiraOpprettOppgave();

		assertThat(Files.exists(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL))).isFalse();
		assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);

		Awaitility.await()
				.atMost(ofSeconds(15))
				.untilAsserted(() -> {
					assertThat(Files.list(sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);
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

	private void preparePath(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		} else {
			FileUtils.cleanDirectory(path.toFile());
		}
	}
}
