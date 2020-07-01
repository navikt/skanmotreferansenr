package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotreferansenr.LesFraFilomraadeOgOpprettJournalpost;
import no.nav.skanmotreferansenr.config.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.filomraade.FilomraadeConsumer;
import no.nav.skanmotreferansenr.filomraade.FilomraadeService;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorConsumer;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.itest.config.TestConfig;
import no.nav.skanmotreferansenr.logiskvedlegg.LeggTilLogiskVedleggConsumer;
import no.nav.skanmotreferansenr.logiskvedlegg.LeggTilLogiskVedleggService;
import no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostConsumer;
import no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostService;
import no.nav.skanmotreferansenr.sftp.Sftp;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wiremock.org.apache.commons.io.FileUtils;
import wiremock.org.apache.commons.io.FilenameUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgOpprettJournalpostIT {

    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";


    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG = "/rest/journalpostapi/v1/dokumentInfo/[0-9]+/logiskVedlegg/";
    private final String URL_FOERSTESIDEGENERATOR_OK_1 = "/api/foerstesidegenerator/v1/foersteside/1111111111111";
    private final String URL_FOERSTESIDEGENERATOR_OK_2 = "/api/foerstesidegenerator/v1/foersteside/2222222222222";
    private final String URL_FOERSTESIDEGENERATOR_NOT_FOUND = "/api/foerstesidegenerator/v1/foersteside/3333333333333";
    private final String URL_STS = "/rest/v1/sts/token";
    private final String FOERSTESIDE_METADATA_HAPPY = "foersteside/foerseside_metadata_HAPPY.json";
    private final String OPPRETT_JOURNALPOST_RESPONSE_HAPPY = "journalpost/opprett_journalpost_response_HAPPY.json";

    private final String ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456789_1_1000";
    private final String LOGISK_VEDLEGG_ID = "885522";

    LesFraFilomraadeOgOpprettJournalpost lesFraFilomraadeOgOpprettJournalpost;
    FilomraadeService filomraadeService;
    OpprettJournalpostService opprettJournalpostService;
    FoerstesidegeneratorService foerstesidegeneratorService;
    LeggTilLogiskVedleggService leggTilLogiskVedleggService;

    @Inject
    private Path sshdPath;

    @Autowired
    SkanmotreferansenrProperties properties;

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        preparePath(inngaaende);
        preparePath(processed);
        preparePath(feilmappe);
        setUpServices();
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void shouldBehandleZip() throws IOException {
        // 09.06.2020_R123456789_1_1000.zip
        // OK   - 09.06.2020_R123456789_0001
        // OK   - 09.06.2020_R123456789_0002 (mangler førstesidemetadata)
        // FEIL - 09.06.2020_R123456789_0003 (valideringsfeil, mangler referansenr)
        // FEIL - 09.06.2020_R123456789_0004 (mangler xml)
        // FEIL - 09.06.2020_R123456789_0005 (mangler pdf)
        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip");
        setUpHappyStubs();

        lesFraFilomraadeOgOpprettJournalpost.lesOgLagreZipfiler();

        assertEquals(4, Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION)).count());
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertTrue(feilmappeContents.containsAll(List.of(
                "09.06.2020_R123456789_0003.pdf",
                "09.06.2020_R123456789_0003.xml",
                "09.06.2020_R123456789_0004.pdf"
        )));
        verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1)));
        verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_OK_2)));
        verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND)));
        verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
        verify(exactly(4), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));

    }

    private void setUpServices() {
        filomraadeService = new FilomraadeService(
                new FilomraadeConsumer(new Sftp(properties), properties)
        );
        opprettJournalpostService = new OpprettJournalpostService(
                new OpprettJournalpostConsumer(new RestTemplateBuilder(), properties),
                new STSConsumer(new RestTemplateBuilder(), properties)
        );
        foerstesidegeneratorService = new FoerstesidegeneratorService(
                new FoerstesidegeneratorConsumer(new RestTemplateBuilder(), properties),
                new STSConsumer(new RestTemplateBuilder(), properties)
        );
        leggTilLogiskVedleggService = new LeggTilLogiskVedleggService(
                new LeggTilLogiskVedleggConsumer(new RestTemplateBuilder(), properties),
                new STSConsumer(new RestTemplateBuilder(), properties)
        );
        lesFraFilomraadeOgOpprettJournalpost = new LesFraFilomraadeOgOpprettJournalpost(
                filomraadeService, foerstesidegeneratorService, opprettJournalpostService, leggTilLogiskVedleggService
        );
    }

    private void preparePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        } else {
            FileUtils.cleanDirectory(path.toFile());
        }
    }

    private void setUpHappyStubs() {
        stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile(OPPRETT_JOURNALPOST_RESPONSE_HAPPY)));
        stubFor(post(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG))
                .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withJsonBody(Json.node(
                                "{\"logiskVedleggId\": \"" + LOGISK_VEDLEGG_ID + "\"}"
                        ))));
        stubFor(post(urlMatching(URL_STS))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        ))));
        stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile(FOERSTESIDE_METADATA_HAPPY)));
        stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_OK_2))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile(FOERSTESIDE_METADATA_HAPPY)));
        stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));
    }

    private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
        Files.copy(new ClassPathResource("__files/" + zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
    }

}