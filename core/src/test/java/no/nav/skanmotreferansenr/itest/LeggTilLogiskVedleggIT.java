package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.LeggTilLogiskVedleggConsumer;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
import no.nav.skanmotreferansenr.consumer.sts.data.STSResponse;
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
    private final String LOGISK_VEDLEGG_ID = "852";
    private final String LEGG_TIL_LOGISK_VEDLEGG_TJENSTE = "/rest/journalpostapi/v1/dokumentInfo/" + DOKUMENT_INFO_ID_OK + "/logiskVedlegg/";
    private final String URL_STS = "/rest/v1/sts/token";

    private LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer;
    private STSConsumer stsConsumer;

    @Autowired
    private SkanmotreferansenrProperties properties;

    @BeforeEach
    void setUpConsumer() {
        setUpStubs();
        stsConsumer = new STSConsumer(new RestTemplateBuilder(), properties);
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
        stubFor(post(urlMatching(URL_STS))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        )))
        );
    }

    @Test
    public void shouldLeggTilLogiskVedlegg() {
        LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder().tittel("Tittel").build();
        STSResponse stsResponse = stsConsumer.getSTSToken();
        LeggTilLogiskVedleggResponse response = leggTilLogiskVedleggConsumer.leggTilLogiskVedlegg(request, DOKUMENT_INFO_ID_OK, stsResponse.getAccess_token());
        assertEquals(LOGISK_VEDLEGG_ID, response.getLogiskVedleggId());
        verify(exactly(1), postRequestedFor(urlMatching(LEGG_TIL_LOGISK_VEDLEGG_TJENSTE)));
    }
}
