package no.nav.skanmotreferansenr.consumer.opprettjournalpost;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.NavHeaders;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.exceptions.functional.OpprettJournalpostFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.OpprettJournalpostTechnicalException;
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
public class OpprettJournalpostConsumer {

    private final RestTemplate restTemplate;
    private final String dokarkivUrl;
    private final String serviceusername;
    private final String REST_JOURNALPOST = "rest/journalpostapi/v1/journalpost";
    private final String QUERY_FERDIGSTILL_FALSE = "foersoekFerdigstill=false";

    public OpprettJournalpostConsumer(RestTemplateBuilder restTemplateBuilder,
                                      SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.serviceusername = skanmotreferansenrProperties.getServiceuser().getUsername();
        this.dokarkivUrl = skanmotreferansenrProperties.getDokarkivurl();
        this.restTemplate = restTemplateBuilder
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "opprettJournalpost"}, percentiles = {0.5, 0.95}, histogram = true)
    public OpprettJournalpostResponse opprettJournalpost(String token, OpprettJournalpostRequest opprettJournalpostRequest) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<OpprettJournalpostRequest> requestEntity = new HttpEntity<>(opprettJournalpostRequest, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
                    .pathSegment(REST_JOURNALPOST)
                    .query(QUERY_FERDIGSTILL_FALSE)
                    .build().toUri();
            return restTemplate.exchange(uri, HttpMethod.POST, requestEntity, OpprettJournalpostResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new OpprettJournalpostFunctionalException(String.format("opprettJournalpost feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new OpprettJournalpostTechnicalException(String.format("opprettJournalpost feilet teknisk med statusKode=%s. Feilmelding=%s", e
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
