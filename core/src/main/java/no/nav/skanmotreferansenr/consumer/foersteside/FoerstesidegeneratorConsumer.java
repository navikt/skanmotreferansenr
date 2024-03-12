package no.nav.skanmotreferansenr.consumer.foersteside;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.HentMetadataFoerstesideTechnicalException;
import no.nav.skanmotreferansenr.filters.NavHeadersFilter;
import no.nav.skanmotreferansenr.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.skanmotreferansenr.consumer.NavHeaders.NAV_CALL_ID;
import static no.nav.skanmotreferansenr.consumer.azure.AzureProperties.CLIENT_REGISTRATION_FOERSTESIDEGENERATOR;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_CALL_ID;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Component
public class FoerstesidegeneratorConsumer {

	private final WebClient webClient;

	public FoerstesidegeneratorConsumer(WebClient webClient, SkanmotreferansenrProperties skanmotreferansenrProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(skanmotreferansenrProperties.getEndpoints().getFoerstesidegenerator().getUrl())
				.filter(new NavHeadersFilter())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.setContentType(APPLICATION_JSON);
				})
				.build();
	}

	@Retryable(retryFor = HentMetadataFoerstesideTechnicalException.class)
	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "hentFoersteside"}, percentiles = {0.5, 0.95}, histogram = true)
	public FoerstesideMetadata hentFoersteside(String loepenr) {
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/api/foerstesidegenerator/v1/foersteside/{loepenr}")
						.build(loepenr))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_FOERSTESIDEGENERATOR))
				.retrieve()
				.bodyToMono(FoerstesideMetadata.class)
				.doOnError(handleFoerstesideErrors(loepenr))
				.block();
	}

	private Consumer<Throwable> handleFoerstesideErrors(String loepenr) {
		return error -> {
			if (error instanceof WebClientResponseException response) {
				if (response.getStatusCode().isSameCodeAs(NOT_FOUND)) {
					throw new HentMetadataFoerstesideFinnesIkkeFunctionalException(format("Fant ikke foersteside med loepenr=%s, status=%s",
							loepenr, response.getStatusCode()), response);
				} else if (response.getStatusCode().value() == CONFLICT.value()) {
					throw new HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException(format("HentMetadataFoersteside feilet funksjonelt med statusKode=%s. Feilmelding=%s",
							response.getStatusCode(), response.getMessage()), response);
				} else if (response.getStatusCode().is5xxServerError()) {
					throw new HentMetadataFoerstesideTechnicalException(format("HentMetadataFoersteside feilet teknisk med statusKode=%s. Feilmelding=%s",
							response.getStatusCode(), response.getMessage()), response);
				} else {
					throw new HentMetadataFoerstesideFunctionalException(format("HentMetadataFoersteside feilet funksjonelt med statusKode=%s. Feilmelding=%s",
							response.getStatusCode(), response.getMessage()), response);
				}
			}
			throw new HentMetadataFoerstesideFunctionalException(format("HentMetadataFoersteside feilet med ukjent funksjonell feil. Feilmelding=%s",
					error.getMessage()), error);
		};
	}
}
