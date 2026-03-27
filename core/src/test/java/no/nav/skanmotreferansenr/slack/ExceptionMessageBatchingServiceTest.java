package no.nav.skanmotreferansenr.slack;

import com.slack.api.methods.SlackApiException;
import no.nav.skanmotreferansenr.config.props.SlackProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static no.nav.skanmotreferansenr.CoreConfig.NORGE_ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

	@Test
	void meldingLagtTilUnderSendingBlirIkkeTapt() throws SlackApiException, IOException {
		service.saveMeldingForBatchedSend("Feil 1");

		// Simuler at en ny melding legges til mens Slack-kallet pågår
		doAnswer(invocation -> {
			service.saveMeldingForBatchedSend("Feil lagt til under sending");
			return null;
		}).when(slackService).sendMelding(anyList());

		service.sendMeldinger();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(slackService, times(1)).sendMelding(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().getFirst()).contains("Feil 1");

		Mockito.reset(slackService);
		service.sendMeldinger();

		verify(slackService, times(1)).sendMelding(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().getFirst()).contains("Feil lagt til under sending");
	}

	@Test
	void meldingerLeggesTilbakeIKoenVedFeil() throws SlackApiException, IOException {
		service.saveMeldingForBatchedSend("Feil som skal overleve");

		doThrow(new IOException("Slack nede")).when(slackService).sendMelding(anyList());

		assertThatThrownBy(() -> service.sendMeldinger()).isInstanceOf(IOException.class);

		// Meldingene skal ha blitt lagt tilbake — neste sending skal inkludere dem
		Mockito.reset(slackService);
		service.sendMeldinger();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(slackService, times(1)).sendMelding(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().getFirst()).contains("Feil som skal overleve");
	}
}
