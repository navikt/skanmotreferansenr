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
import no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostConsumer;
import no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostService;
import no.nav.skanmotreferansenr.sftp.Sftp;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgOpprettJournalpostIT {

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String URL_FOERSTESIDEGENERATOR = "/api/foerstesidegenerator/v1/foersteside/\\d{13}";
    private final String URL_FOERSTESIDEGENERATOR_NOT_FOUND = "/api/foerstesidegenerator/v1/foersteside/11111111111111";
    private final String STSUrl = "/rest/v1/sts/token";
    private static final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";
    private final String FOERSTESIDE_METADATA_HAPPY = "foersteside/foerseside_metadata_HAPPY.json";

    private final Path MOCKZIP = Path.of("src/test/resources/__files/xml_pdf_pairs/xml_pdf_pairs_testdata.zip");
    private final Path SKANMOTREFERANSENR_ZIP_PATH = Path.of("src/test/resources/inbound/xml_pdf_pairs_testdata.zip");

    LesFraFilomraadeOgOpprettJournalpost lesFraFilomraadeOgOpprettJournalpost;
    FilomraadeService filomraadeService;
    OpprettJournalpostService opprettJournalpostService;
    FoerstesidegeneratorService foerstesidegeneratorService;

    private int PORT = 2222;
    private SshServer sshd = SshServer.setUpDefaultServer();
    private Sftp sftp;

    @Autowired
    SkanmotreferansenrProperties properties;

    @BeforeAll
    void startSftpServer() throws IOException {
        sshd.setPort(PORT);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("src/test/resources/sftp/itest.ser")));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get(VALID_PUBLIC_KEY_PATH)));
        sshd.start();
    }

    @AfterAll
    void shutdownSftpServer() throws IOException {
        sshd.stop();
        sshd.close();
    }

    @BeforeEach
    void setUpServices() {
        sftp = new Sftp(properties);
        filomraadeService = new FilomraadeService(new FilomraadeConsumer(sftp, properties));
        opprettJournalpostService = new OpprettJournalpostService(
                new OpprettJournalpostConsumer(new RestTemplateBuilder(), properties),
                new STSConsumer(new RestTemplateBuilder(), properties)
        );
        foerstesidegeneratorService = new FoerstesidegeneratorService(
                new FoerstesidegeneratorConsumer(new RestTemplateBuilder(), properties),
                new STSConsumer(new RestTemplateBuilder(), properties));
        lesFraFilomraadeOgOpprettJournalpost = new LesFraFilomraadeOgOpprettJournalpost(filomraadeService, foerstesidegeneratorService, opprettJournalpostService);
        copyFileToSkanmotreferansenrFolder();
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpHappyStubs() {
        stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
        stubFor(post(urlMatching(STSUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        )))
        );
        stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(FOERSTESIDE_METADATA_HAPPY)));
        stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));
    }

    private void setUpBadStubs() {
        stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
        stubFor(post(urlMatching(STSUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        )))
        );
    }

    @Test
    public void shouldLesOgLagreHappy() {
        setUpHappyStubs();
        try {
            lesFraFilomraadeOgOpprettJournalpost.lesOgLagreZipfiler();
            verify(exactly(3), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
        } catch (Exception e) {
            fail();
        }
    }

    /*
    @Test
    public void shouldMoveFilesWhenBadRequest() {
        setUpBadStubs();
        lesFraFilomraadeOgOpprettJournalpost.lesOgLagre();
        assertTrue("foo".equals("bar"));
    }

     */

    private void copyFileToSkanmotreferansenrFolder() {
        try {
            Path source = MOCKZIP;
            Path dest = SKANMOTREFERANSENR_ZIP_PATH;
            Files.copy(source, dest);
        } catch (IOException ignored) {
            // File either already exists or the test will crash and burn
        }
    }
}