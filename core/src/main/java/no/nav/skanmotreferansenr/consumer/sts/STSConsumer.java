package no.nav.skanmotreferansenr.consumer.sts;


import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.NavHeaders;
import no.nav.skanmotreferansenr.consumer.sts.data.STSResponse;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrStsFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.SkanmotreferansenrStsTechnicalException;
import no.nav.skanmotreferansenr.mdc.MDCConstants;
import no.nav.skanmotreferansenr.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static no.nav.skanmotreferansenr.config.LocalCacheConfig.STS_CACHE;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;


@Component
public class STSConsumer {
    private static final String URL_ENCODED_BODY = "grant_type=client_credentials&scope=openid";

    private final RestTemplate restTemplate;
    private final String stsUrl;
    private final String serviceusername;


    public STSConsumer(RestTemplateBuilder restTemplateBuilder,
                       SkanmotreferansenrProperties skanmotreferansenrProperties) {
        this.serviceusername = skanmotreferansenrProperties.getServiceuser().getUsername();
        this.stsUrl = skanmotreferansenrProperties.getStsurl();
        this.restTemplate = restTemplateBuilder
                .basicAuthentication(skanmotreferansenrProperties.getServiceuser().getUsername(),
                        skanmotreferansenrProperties.getServiceuser().getPassword())
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "getSTSToken"}, percentiles = {0.5, 0.95}, histogram = true)
    @Cacheable(STS_CACHE)
    public STSResponse getSTSToken() {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(URL_ENCODED_BODY, headers);

            return restTemplate.exchange(stsUrl, HttpMethod.POST, requestEntity, STSResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new SkanmotreferansenrStsFunctionalException(String.format("getSTSToken feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new SkanmotreferansenrStsTechnicalException(String.format("getSTSToken feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }


    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (MDC.get(MDCConstants.MDC_CALL_ID) != null) {
            headers.add(NavHeaders.NAV_CALL_ID, MDC.get(MDCConstants.MDC_CALL_ID));
        }
        headers.add(NavHeaders.NAV_CONSUMER_ID, serviceusername);
        return headers;
    }
}