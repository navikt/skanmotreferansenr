package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.itest.config.TestConfig;
import no.nav.skanmotreferansenr.logiskvedlegg.LeggTilLogiskVedleggConsumer;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class LeggTilLogiskVedleggIT {

    private final String DOKUMENT_INFO_ID_OK = "885522";
    private final String LEGG_TIL_LOGISK_VEDLEGG_TJENSTE = "/rest/journalpostapi/v1/dokumentInfo/" + DOKUMENT_INFO_ID_OK + "/logiskVedlegg";
    private final String LOGISK_VEDLEGG_ID = "852";

    private LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer;

    @Autowired
    private SkanmotreferansenrProperties properties;

    @BeforeEach
    void setUpConsumer() {
        setUpStubs();
        leggTilLogiskVedleggConsumer = new LeggTilLogiskVedleggConsumer(new RestTemplateBuilder(), properties);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpStubs() {
        stubFor(post(urlMatching(LEGG_TIL_LOGISK_VEDLEGG_TJENSTE))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"logiskVedleggId\": \"" + LOGISK_VEDLEGG_ID + "\"}"
                        )))
        );
    }

    @Test
    public void shouldLeggTilLogiskVedlegg() {
        LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder().tittel("Tittel").build();
        LeggTilLogiskVedleggResponse response = leggTilLogiskVedleggConsumer.leggTilLogiskVedlegg(request, DOKUMENT_INFO_ID_OK);
        assertEquals(LOGISK_VEDLEGG_ID, response.getLogiskVedleggId());
        verify(exactly(1), postRequestedFor(urlMatching(LEGG_TIL_LOGISK_VEDLEGG_TJENSTE)));
    }
}
