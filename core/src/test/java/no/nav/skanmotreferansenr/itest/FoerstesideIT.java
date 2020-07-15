package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.foersteside.FoerstesidegeneratorConsumer;
import no.nav.skanmotreferansenr.consumer.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
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
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class FoerstesideIT {

    private final String LOEPENR_OK = "111";
    private final String LOEPENR_NOT_FOUND = "222";
    private final String HENT_FOERSTESIDE_METADATA = "/api/foerstesidegenerator/v1/foersteside/";
    private final String STS_URL = "/rest/v1/sts/token";
    private final String METADATA_HAPPY = "foersteside/foersteside_HAPPY.json";

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
        stubFor(get(urlMatching(HENT_FOERSTESIDE_METADATA + LOEPENR_OK))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(METADATA_HAPPY)));
        stubFor(get(urlMatching(HENT_FOERSTESIDE_METADATA + LOEPENR_NOT_FOUND))
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
        FoerstesideMetadata metadata = foerstesidegeneratorService.hentFoersteside(LOEPENR_OK).orElse(new FoerstesideMetadata());

        assertNull(metadata.getAvsender());
        assertEquals("12345678910", metadata.getBruker().getBrukerId());
        assertEquals("PERSON", metadata.getBruker().getBrukerType());
        assertEquals("AAP", metadata.getTema());
        assertNull(metadata.getBehandlingstema());
        assertEquals("Brev", metadata.getArkivtittel());
        assertEquals("VANL", metadata.getNavSkjemaId());
        assertEquals("9999", metadata.getEnhetsnummer());
        assertEquals(2, metadata.getVedleggsliste().size());
        assertTrue(metadata.getVedleggsliste().containsAll(List.of("Terminbekreftelse", "Dokumentasjon av inntekt")));
    }

    @Test
    void shouldGetEmptyOptionalIfNotExisting() {
        assertThat(foerstesidegeneratorService.hentFoersteside("222")).isEmpty();
    }
}