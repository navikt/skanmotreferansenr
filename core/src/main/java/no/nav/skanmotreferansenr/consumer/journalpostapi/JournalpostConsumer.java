package no.nav.skanmotreferansenr.consumer.journalpostapi;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.AvstemmingReferanser;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.FeilendeAvstemmingReferanser;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.codec.CodecProperties;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.skanmotreferansenr.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.skanmotreferansenr.consumer.RetryConstants.MAX_RETRIES;
import static no.nav.skanmotreferansenr.consumer.RetryConstants.RETRY_DELAY;
import static no.nav.skanmotreferansenr.consumer.azure.AzureOAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_CALL_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static reactor.core.publisher.Mono.just;

@Slf4j
@Component
public class JournalpostConsumer {

	private final WebClient webClient;

	public JournalpostConsumer(WebClient webClient,
							   SkanmotreferansenrProperties skanmotreferansenrProperties,
							   CodecProperties codecProperties
	) {
		this.webClient = webClient.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(skanmotreferansenrProperties.getEndpoints().getDokarkiv().getUrl())
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
								.maxInMemorySize((int) codecProperties.getMaxInMemorySize().toBytes()))
						.build())
				.build();
	}

	@Retryable(retryFor = SkanmotreferansenrTechnicalException.class,
			maxAttempts = MAX_RETRIES, backoff = @Backoff(delay = RETRY_DELAY))
	public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest opprettJournalpostRequest) {
		return webClient.post()
				.uri("/journalpost?foersoekFerdigstill=false")
				.header(NAV_CALL_ID, MDC.get(MDC_CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.bodyValue(opprettJournalpostRequest)
				.retrieve()
				.bodyToMono(OpprettJournalpostResponse.class)
				.onErrorMap(WebClientResponseException.class, err -> handleOpprettJournalpostError(err, opprettJournalpostRequest.getEksternReferanseId()))
				.block();
	}

	@Retryable(maxAttempts = MAX_RETRIES, backoff = @Backoff(delay = RETRY_DELAY))
	public LeggTilLogiskVedleggResponse leggTilLogiskVedlegg(
			LeggTilLogiskVedleggRequest request,
			String dokumentInfoId
	) {
		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/")
						.build(dokumentInfoId))
				.header(NAV_CALL_ID, MDC.get(MDC_CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.bodyValue(request)
				.retrieve()
				.bodyToMono(LeggTilLogiskVedleggResponse.class)
				.onErrorMap(WebClientResponseException.class, err -> handleError(err, "leggTilLogiskVedlegg"))
				.block();
	}

	@Retryable(retryFor = SkanmotreferansenrTechnicalException.class, backoff = @Backoff(delay = RETRY_DELAY))
	public FeilendeAvstemmingReferanser avstemReferanser(AvstemmingReferanser avstemmingReferanser) {

		return webClient.post()
				.uri("/avstemReferanser")
				.header(NAV_CALL_ID, MDC.get(MDC_CALL_ID))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.body(just(avstemmingReferanser), AvstemmingReferanser.class)
				.retrieve()
				.bodyToMono(FeilendeAvstemmingReferanser.class)
				.onErrorMap(WebClientResponseException.class, err -> handleError(err, "avstemReferanser"))
				.block();
	}

	private Throwable handleOpprettJournalpostError(WebClientResponseException webException, String eksternReferanseId) {
		if (webException.getStatusCode().is4xxClientError()) {
			if (CONFLICT.equals(webException.getStatusCode())) {
				OpprettJournalpostResponse opprettJournalpostResponse = webException.getResponseBodyAs(OpprettJournalpostResponse.class);
				log.info("Det eksisterer allerede en journalpost i dokarkiv med journalpostId={}, eksternReferanseId={} og kan ikke opprette ny journalpost.",
						opprettJournalpostResponse.getJournalpostId(), eksternReferanseId);
				throw new SkanmotreferansenrFunctionalException(format("Det eksisterer allerede en journalpost i dokarkiv med journalpostId=%s. Feilmelding=%s",
						opprettJournalpostResponse.getJournalpostId(), webException.getMessage()), webException);
			}
			throw new SkanmotreferansenrFunctionalException(format("opprettJournalpost feilet funksjonelt med statusKode=%s. Feilmelding=%s",
					webException.getStatusCode(), webException.getMessage()), webException);

		}
		throw new SkanmotreferansenrTechnicalException(format("opprettJournalpost feilet teknisk med Feilmelding=%s", webException.getMessage()), webException);
	}

	private Throwable handleError(WebClientResponseException webException, String tjeneste) {
		if (webException.getStatusCode().is4xxClientError()) {
			throw new SkanmotreferansenrFunctionalException(format("%s feilet funksjonelt med statusKode=%s. Feilmelding=%s", tjeneste,
					webException.getStatusCode(), webException.getMessage()), webException);
		}
		throw new SkanmotreferansenrTechnicalException(format("%s feilet teknisk med Feilmelding=%s", tjeneste, webException.getMessage()), webException);
	}
}
