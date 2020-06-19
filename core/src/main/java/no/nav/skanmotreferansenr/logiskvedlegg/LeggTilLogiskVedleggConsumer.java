package no.nav.skanmotreferansenr.logiskvedlegg;

import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.functional.OpprettJournalpostFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.OpprettJournalpostTechnicalException;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
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
    private final String dokarkivDokumentinfoUrl;
    private final String LEGG_TIL_LOGISK_VEDLEGG_TJENESTE = "logiskVedlegg";

    public LeggTilLogiskVedleggConsumer(RestTemplateBuilder restTemplateBuilder,
                                        SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.dokarkivDokumentinfoUrl = skanmotreferansenrProperties.getDokarkivdokumentinfourl();
        this.restTemplate = restTemplateBuilder
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "opprettJournalpost"}, percentiles = {0.5, 0.95}, histogram = true)
    public LeggTilLogiskVedleggResponse leggTilLogiskVedlegg(LeggTilLogiskVedleggRequest request, String dokumentInfoId, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<LeggTilLogiskVedleggRequest> requestEntity = new HttpEntity<>(request, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(dokarkivDokumentinfoUrl)
                    .pathSegment(dokumentInfoId, LEGG_TIL_LOGISK_VEDLEGG_TJENESTE)
                    .build().toUri();
            return restTemplate.exchange(uri, HttpMethod.POST, requestEntity, LeggTilLogiskVedleggResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new OpprettJournalpostFunctionalException(String.format("leggTilLogiskVedlegg feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new OpprettJournalpostTechnicalException(String.format("leggTilLogiskVedlegg feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }


    private HttpHeaders createHeaders(String token) {
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
