package no.nav.skanmotreferansenr.itest;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles({"itest", "fridag"})
public class AvstemRouteFridagIT extends AbstractItest {

	private static final String AVSTEMMINGSFILMAPPE = "avstemmappe";
	private static final String PROCESSED = "processed";
	private static final String AVSTEMMINGSFIL = "04.01.2024_avstemmingsfil_1.txt";

	@Autowired
	private Path sshdPath;

	@BeforeAll
	public static void beforeTestClass() {
		System.setProperty("skanmotreferansenr.sftp.port", String.valueOf(RandomUtils.secure().randomInt(2000, 65000)));
	}

	@BeforeEach
	void beforeEach() {
		super.setUpStubs();
		final Path avstem = sshdPath.resolve(AVSTEMMINGSFILMAPPE);
		final Path processed = avstem.resolve(PROCESSED);
		preparePath(avstem);
		preparePath(processed);
	}

	@Test
	public void shouldNotOpprettJiraOppgaveWhenAvstemmingsfilIsMissingAndForrigevirkedagIsOffentligFridag() {
		stubBadRequestJiraOpprettOppgave();

		Path filePath = sshdPath.resolve(AVSTEMMINGSFILMAPPE).resolve(AVSTEMMINGSFIL);
		assertThat(Files.exists(filePath)).isFalse();
		assertAntallProsesserteFiler(0);

			await()
					.pollDelay(ofSeconds(2))
					.atMost(ofSeconds(20))
					.untilAsserted(() -> {
						assertAntallProsesserteFiler(0);
						verify(0, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));
						verify(0, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
					});
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

}
