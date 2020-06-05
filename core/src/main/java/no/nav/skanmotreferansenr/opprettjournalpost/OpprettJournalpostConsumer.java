package no.nav.skanmotreferansenr.opprettjournalpost;

import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.functional.OpprettJournalpostFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.OpprettJournalpostTechnicalException;
import no.nav.skanmotreferansenr.mdc.MDCConstants;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
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

import java.net.URI;
import java.net.URISyntaxException;

import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;


@Component
public class OpprettJournalpostConsumer {

    private final RestTemplate restTemplate;
    private final String dokarkivJournalpostUrl;

    public OpprettJournalpostConsumer(RestTemplateBuilder restTemplateBuilder,
                                      SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.dokarkivJournalpostUrl = skanmotreferansenrProperties.getDokarkivjournalposturl();
        this.restTemplate = restTemplateBuilder
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "opprettJournalpost"}, percentiles = {0.5, 0.95}, histogram = true)
    public OpprettJournalpostResponse opprettJournalpost(String token, OpprettJournalpostRequest opprettJournalpostRequest) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<OpprettJournalpostRequest> requestEntity = new HttpEntity<>(opprettJournalpostRequest, headers);

            URI uri = new URI(dokarkivJournalpostUrl);
            return restTemplate.exchange(uri, HttpMethod.POST, requestEntity, OpprettJournalpostResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new OpprettJournalpostFunctionalException(String.format("opprettJournalpost feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new OpprettJournalpostTechnicalException(String.format("opprettJournalpost feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (URISyntaxException e) {
            throw new OpprettJournalpostTechnicalException(String.format("opprettJournalpost feilet teknisk. Feilmelding=%s",
                    e.getMessage()), e);
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
