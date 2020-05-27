package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotreferansenr.config.properties.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorConsumer;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.foersteside.data.Bruker;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.itest.config.TestConfig;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class FoerstesideIT {

    private final String HENT_FOERSTESIDE_METADATA_OK = "/api/foerstesidegenerator/v1/foersteside/111";
    private final String HENT_FOERSTESIDE_METADATA_NOT_FOUND = "/api/foerstesidegenerator/v1/foersteside/222";
    private final String STS_URL = "/rest/v1/sts/token";
    private final String METADATA_HAPPY = "foersteside/foerseside_metadata_HAPPY.json";

    private FoerstesidegeneratorService foerstesidegeneratorService;

    @Inject
    private SkanmotreferansenrProperties skanmotreferansenrProperties;

    @BeforeEach
    void setUp() {
        setUpStubs();
        foerstesidegeneratorService = new FoerstesidegeneratorService(
                new FoerstesidegeneratorConsumer(new RestTemplateBuilder(), skanmotreferansenrProperties),
                new STSConsumer(new RestTemplateBuilder(), skanmotreferansenrProperties)
        );
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpStubs() {
        stubFor(get(urlMatching(HENT_FOERSTESIDE_METADATA_OK))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(METADATA_HAPPY)));
        stubFor(get(urlMatching(HENT_FOERSTESIDE_METADATA_NOT_FOUND))
                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
        stubFor(post(urlMatching(STS_URL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        )))
        );
    }

    @Test
    void shouldGetFoerstesideMetadata() {
        FoerstesideMetadata metadata = foerstesidegeneratorService.hentFoersteside("111").get();

        assertNull(metadata.getAvsender());
        assertEquals("12345678910", metadata.getBruker().getBrukerId());
        assertEquals(Bruker.BrukerType.PERSON, metadata.getBruker().getBrukerType());
        assertEquals("AAP", metadata.getTema());
        assertNull(metadata.getBehandlingstema());
        assertEquals("Brev", metadata.getArkivtittel());
        assertEquals("VANL", metadata.getNavSkjemaId());
        assertEquals("9999", metadata.getEnhetsnummer());
    }

    @Test
    void shouldGetNullIfNotExisting() {
        Optional<FoerstesideMetadata> metadata = foerstesidegeneratorService.hentFoersteside("222");
        assertTrue(metadata.isEmpty());
    }

}
