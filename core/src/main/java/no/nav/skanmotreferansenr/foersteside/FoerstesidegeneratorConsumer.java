package no.nav.skanmotreferansenr.foersteside;

import no.nav.skanmotreferansenr.config.properties.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.HentMetadataFoerstesideTechnicalException;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.mdc.MDCConstants;
import no.nav.skanmotreferansenr.mdc.MDCGenerate;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Component
public class FoerstesidegeneratorConsumer {

    private final RestTemplate restTemplate;
    private final String foerstesideUrl;

    public FoerstesidegeneratorConsumer(RestTemplateBuilder restTemplateBuilder, SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.foerstesideUrl = skanmotreferansenrProperties.getGetmetadatafoerstesideurl();
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(5))
                .setConnectTimeout(Duration.ofSeconds(2))
                .basicAuthentication(skanmotreferansenrProperties.getServiceuser().getUsername(),
                        skanmotreferansenrProperties.getServiceuser().getPassword())
                .build();
    }

    public FoerstesideMetadata hentFoersteside(String token, String loepenr) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            URI uri = UriComponentsBuilder.fromHttpUrl(foerstesideUrl).pathSegment(loepenr).build().toUri();

            return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, FoerstesideMetadata.class).getBody();

        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new HentMetadataFoerstesideFinnesIkkeFunctionalException(String.format("HentMetadataFoersteside feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
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
        MDCGenerate.generateNewCallIdIfThereAreNone();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        if (MDC.get(MDCConstants.MDC_NAV_CALL_ID) != null) {
            headers.add(MDCConstants.MDC_NAV_CALL_ID, MDC.get(MDCConstants.MDC_NAV_CALL_ID));
        }
        if (MDC.get(MDCConstants.MDC_NAV_CONSUMER_ID) != null) {
            headers.add(MDCConstants.MDC_NAV_CONSUMER_ID, MDC.get(MDCConstants.MDC_NAV_CONSUMER_ID));
        }
        return headers;
    }
}
