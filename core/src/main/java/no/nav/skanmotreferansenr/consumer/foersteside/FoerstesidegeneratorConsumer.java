package no.nav.skanmotreferansenr.consumer.foersteside;

import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.NavHeaders;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.HentMetadataFoerstesideTechnicalException;
import no.nav.skanmotreferansenr.mdc.MDCConstants;
import no.nav.skanmotreferansenr.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;

@Component
public class FoerstesidegeneratorConsumer {

    private final RestTemplate restTemplate;
    private final String foerstesideUrl;
    private final String serviceusername;

    public FoerstesidegeneratorConsumer(RestTemplateBuilder restTemplateBuilder, SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.serviceusername = skanmotreferansenrProperties.getServiceuser().getUsername();
        this.foerstesideUrl = skanmotreferansenrProperties.getGetmetadatafoerstesideurl();
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(5))
                .setConnectTimeout(Duration.ofSeconds(2))
                .basicAuthentication(skanmotreferansenrProperties.getServiceuser().getUsername(),
                        skanmotreferansenrProperties.getServiceuser().getPassword())
                .build();
    }

    @Retryable(include = HentMetadataFoerstesideTechnicalException.class)
    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "hentFoersteside"}, percentiles = {0.5, 0.95}, histogram = true)
    public FoerstesideMetadata hentFoersteside(String token, String loepenr) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            URI uri = UriComponentsBuilder.fromHttpUrl(foerstesideUrl).pathSegment(loepenr).build().toUri();

            return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, FoerstesideMetadata.class).getBody();
        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new HentMetadataFoerstesideFinnesIkkeFunctionalException(String.format("Fant ikke foersteside med loepenr=%s, status=%s",
                        loepenr, e.getStatusCode()), e);
            } else if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
                throw new HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException(String.format("HentMetadataFoersteside feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            } else {
                throw new HentMetadataFoerstesideFunctionalException(String.format("HentMetadataFoersteside feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            }
        } catch (HttpServerErrorException e) {
            throw new HentMetadataFoerstesideTechnicalException(String.format("HentMetadataFoersteside feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        if (MDC.get(MDCConstants.MDC_CALL_ID) != null) {
            headers.add(NavHeaders.NAV_CALL_ID, MDC.get(MDCConstants.MDC_CALL_ID));
        }
        headers.add(NavHeaders.NAV_CONSUMER_ID, serviceusername);
        return headers;
    }
}
