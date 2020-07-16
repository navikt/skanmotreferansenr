package no.nav.skanmotreferansenr.consumer.logiskvedlegg;

import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.NavHeaders;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.exceptions.functional.LeggTilLogiskVedleggFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.LeggTilLogiskVedleggTechnicalException;
import no.nav.skanmotreferansenr.mdc.MDCConstants;
import no.nav.skanmotreferansenr.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;

@Component
public class LeggTilLogiskVedleggConsumer {

    private final RestTemplate restTemplate;
    private final String dokarkivUrl;
    private final String serviceusername;
    private final String REST_DOKUMENTINFO = "rest/journalpostapi/v1/dokumentInfo";
    private final String LEGG_TIL_LOGISK_VEDLEGG_TJENESTE = "logiskVedlegg/";

    public LeggTilLogiskVedleggConsumer(RestTemplateBuilder restTemplateBuilder,
                                        SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.dokarkivUrl = skanmotreferansenrProperties.getDokarkivurl();
        this.restTemplate = restTemplateBuilder
                .build();
        this.serviceusername = skanmotreferansenrProperties.getServiceuser().getUsername();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "leggTilLogiskVedlegg"}, percentiles = {0.5, 0.95}, histogram = true)
    public LeggTilLogiskVedleggResponse leggTilLogiskVedlegg(LeggTilLogiskVedleggRequest request, String dokumentInfoId, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<LeggTilLogiskVedleggRequest> requestEntity = new HttpEntity<>(request, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
                    .pathSegment(REST_DOKUMENTINFO, dokumentInfoId, LEGG_TIL_LOGISK_VEDLEGG_TJENESTE)
                    .build().toUri();
            return restTemplate.exchange(uri, HttpMethod.POST, requestEntity, LeggTilLogiskVedleggResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new LeggTilLogiskVedleggFunctionalException(String.format("leggTilLogiskVedlegg feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new LeggTilLogiskVedleggTechnicalException(String.format("leggTilLogiskVedlegg feilet teknisk med statusKode=%s. Feilmelding=%s", e
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
