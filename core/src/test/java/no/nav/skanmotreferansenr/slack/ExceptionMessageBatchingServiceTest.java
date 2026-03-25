package no.nav.skanmotreferansenr.slack;

import com.slack.api.methods.SlackApiException;
import no.nav.skanmotreferansenr.config.props.SlackProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import static no.nav.skanmotreferansenr.CoreConfig.NORGE_ZONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

class ExceptionMessageBatchingServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), NORGE_ZONE);

	private SlackProperties slackProperties;
	private SlackService slackService;
	private ExceptionMessageBatchingService service;

	@BeforeEach
	void setUp() {
		slackProperties = mock(SlackProperties.class);
		slackService = mock(SlackService.class);

		when(slackProperties.alertsEnabled()).thenReturn(true);

		service = new ExceptionMessageBatchingService(slackProperties, slackService, FIXED_CLOCK);
	}

	@Test
	void destroySenderMeldingerNaarKoenIkkeErTom() throws SlackApiException, IOException {
		service.saveMeldingForBatchedSend("Noe gikk galt");

		service.destroy();

		verify(slackService, times(1)).sendMelding(anyList());
	}

	@Test
	void destroySenderIkkeMeldingerNaarKoenErTom() {
		service.destroy();

		verifyNoInteractions(slackService);
	}

	@Test
	void sendMeldingerToemmerKoen() throws SlackApiException, IOException {
		service.saveMeldingForBatchedSend("Feil 1");
		service.saveMeldingForBatchedSend("Feil 2");

		service.sendMeldinger();

		// Andre kall skal ikke sende fordi køen er tømt
		service.sendMeldinger();

		verify(slackService, times(1)).sendMelding(anyList());
	}

	@Test
	void sendMeldingerGjoerIngentingNaarKoenErTom() throws SlackApiException, IOException {
		service.sendMeldinger();

		verifyNoInteractions(slackService);
	}

	@Test
	void destroyLoggerOgSenderVedFeil() throws SlackApiException, IOException {
		service.saveMeldingForBatchedSend("Viktig feilmelding");

		// Simuler at Slack er nede
		org.mockito.Mockito.doThrow(new IOException("Slack nede")).when(slackService).sendMelding(anyList());

		// Skal ikke kaste exception — destroy() håndterer feil via sendMeldingImmediately()
		service.destroy();

		verify(slackService, times(1)).sendMelding(anyList());
	}
}
