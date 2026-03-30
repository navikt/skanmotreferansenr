package no.nav.skanmotreferansenr.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.props.SlackProperties;
import no.nav.skanmotreferansenr.exceptions.technical.SlackServiceException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SlackService {

	private final MethodsClient slackClient;
	private final SlackProperties slackProperties;

	SlackService(SlackProperties slackProperties,
	             MethodsClient slackClient) {
		this.slackProperties = slackProperties;
		this.slackClient = slackClient;
	}

	public void sendMelding(List<String> melding) throws SlackApiException, IOException {
		if (slackProperties.alertsEnabled()) {
			log.info("Sender melding til Slack med melding={}", melding);

			var response = slackClient.chatPostMessage(jobbFeiletMelding(melding));
			if (response.isOk()) {
				log.info("Sendte melding med ts={} til Slack med resultat=OK", response.getTs());
			} else {
				throw new SlackServiceException("Sending til slack feilet: %s".formatted(response.getError()));
			}
		}
	}

	private ChatPostMessageRequest jobbFeiletMelding(List<String> feilmelding) {
		String headerText = ":rotating_light: Skedulert jobb feilet!";
		String feilmeldingerSomListe = feilmelding.stream().map(s -> "\n  - " + s).collect(Collectors.joining());
		String bodyText = """
			*Applikasjon:* skanmotreferansenr
			*Feilmelding:* %s
			""".formatted(feilmeldingerSomListe);

		return ChatPostMessageRequest.builder()
			.channel(slackProperties.channel())
			.text(bodyText) //ved bruk av blocks fungerer dette som fallback-tekst for varsel
			.blocks(List.of(
				HeaderBlock.builder()
					.text(PlainTextObject.builder().text(headerText).build())
					.build(),
				SectionBlock.builder()
					.text(MarkdownTextObject.builder().text(bodyText).build())
					.build()
			))
			.build();
	}
}
