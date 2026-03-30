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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@Slf4j
@Service
public class ExceptionMessageBatchingService {
	private final SlackProperties slackProperties;
	private final ConcurrentSkipListMap<String,Integer> feilmeldingerIkkePostet = new ConcurrentSkipListMap<>();
	private final SlackService slackService;

	ExceptionMessageBatchingService(SlackProperties slackProperties,
				 SlackService slackService) {
		this.slackProperties = slackProperties;
		this.slackService = slackService;
	}

	@Scheduled(cron = "${skanmotreferansenr.slack-varsel-cron}")
	@Retryable(retryFor =  {SlackApiException.class, IOException.class}, backoff = @Backoff(multiplier =  2, delay = 1000))
	public void sendMeldinger() throws SlackApiException, IOException {
		sendMeldingInternal();
	}

	@PreDestroy
	void destroy() {
		if (!feilmeldingerIkkePostet.isEmpty()) {
			log.info("Applikasjonen stenges — sender {} usendte feilmeldinger til Slack", feilmeldingerIkkePostet.size());
			try {
				sendMeldingInternal();
			} catch (Exception e) {
				log.error("Sending av melding til Slack feilet med feilmelding={}", e.getMessage(), e);
			}
		}
	}

	private void sendMeldingInternal() throws SlackApiException, IOException {
		var feilmeldinger = getSavedFeilmeldinger();
		if (feilmeldinger.isEmpty()) {
			return;
		}
		try {
			if (slackProperties.alertsEnabled()) {
				slackService.sendMelding(feilmeldinger.stream()
					.map(entry -> "%s: %d ganger".formatted(entry.getKey(), entry.getValue()))
					.toList());
				log.info("Sender melding til Slack med melding={}", feilmeldinger);
			} else {
				log.info("Varsling til Slack er deaktivert. Sender ikke melding={}", feilmeldinger);
			}
		} catch (SlackApiException | IOException | RuntimeException e) {
			// Legg meldingene tilbake i køen slik at de kan sendes ved neste forsøk
			feilmeldinger.forEach(entry ->
				feilmeldingerIkkePostet.merge(entry.getKey(), entry.getValue(), Integer::sum));
			throw e;
		}
	}

	public synchronized void saveMeldingForBatchedSend(String feilmelding) {
		feilmeldingerIkkePostet.merge(feilmelding, 1, Integer::sum);
	}

	private List<Map.Entry<String,Integer>> getSavedFeilmeldinger() {
		var meldinger = new ArrayList<Map.Entry<String,Integer>>();
		Map.Entry<String,Integer> entry;
		while ((entry = feilmeldingerIkkePostet.pollFirstEntry()) != null) {
			meldinger.add(entry);
		}
		return meldinger;
	}
}
