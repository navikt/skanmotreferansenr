package no.nav.skanmotreferansenr.slack;

import com.slack.api.methods.SlackApiException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.props.SlackProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class ExceptionMessageBatchingService {
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SlackProperties slackProperties;
	private final Clock clock;
	private final Queue<String> feilmeldingerIkkePostet = new ConcurrentLinkedQueue<>();
	private final SlackService slackService;

	ExceptionMessageBatchingService(SlackProperties slackProperties,
				 SlackService slackService,
				 Clock clock) {
		this.slackProperties = slackProperties;
		this.clock = clock;
		this.slackService = slackService;
	}

	@Scheduled(cron = "${skanmotreferansenr.slack-varsel-cron}")
	@Retryable(retryFor =  {SlackApiException.class, IOException.class}, backoff = @Backoff(multiplier =  2, delay = 1000))
	public void sendMeldinger() throws SlackApiException, IOException {
		sendMeldingInternal();
	}

	public void sendMeldingImmediately() {
		try {
			sendMeldingInternal();
		} catch (Exception e) {
			log.error("Sending av melding til Slack feilet med feilmelding={}", e.getMessage(), e);
		}
	}

	@PreDestroy
	void destroy() {
		if (!feilmeldingerIkkePostet.isEmpty()) {
			log.info("Applikasjonen stenges — sender {} usendte feilmeldinger til Slack", feilmeldingerIkkePostet.size());
			sendMeldingImmediately();
		}
	}

	private void sendMeldingInternal() throws SlackApiException, IOException {
		if (feilmeldingerIkkePostet.isEmpty()) {
			return;
		}
		var melding = getSavedFeilmeldinger();
		if (slackProperties.alertsEnabled()) {
			slackService.sendMelding(melding);
			log.info("Sender melding til Slack med melding={}", melding);
		} else {
			log.info("Varsling til Slack er deaktivert. Sender ikke melding={}", melding);
		}
		feilmeldingerIkkePostet.clear();
	}

	public void saveMeldingForBatchedSend(String feilmelding) {
		feilmeldingerIkkePostet.add("_%s_: %s".formatted(DATE_TIME_FORMATTER.format(ZonedDateTime.now(clock)), feilmelding));
	}

	private List<String> getSavedFeilmeldinger() {
		return List.of(feilmeldingerIkkePostet.toArray(String[]::new));
	}
}
