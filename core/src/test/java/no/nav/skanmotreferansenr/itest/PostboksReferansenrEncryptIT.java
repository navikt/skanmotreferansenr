package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.nio.file.NoSuchFileException;
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
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.cloud.vault.token=123456")
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class PostboksReferansenrEncryptIT {

    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";


    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG = "/rest/journalpostapi/v1/dokumentInfo/[0-9]+/logiskVedlegg/";
    private final String URL_FOERSTESIDEGENERATOR_OK_1 = "/api/foerstesidegenerator/v1/foersteside/1111111111111";
    private final String URL_FOERSTESIDEGENERATOR_NOT_FOUND = "/api/foerstesidegenerator/v1/foersteside/2222222222222";
    private final String URL_STS = "/rest/v1/sts/token";
    private final String FOERSTESIDE_METADATA_HAPPY = "foersteside/foersteside_HAPPY.json";
    private final String OPPRETT_JOURNALPOST_RESPONSE_HAPPY = "journalpost/opprett_journalpost_response_HAPPY.json";
    private final String LOGISK_VEDLEGG_ID = "885522";

    private final String ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456780_1_2000";
    private final String UNENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456781_2_1000";
    private final String FAIL_ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION = "09.06.2020_R123456783_3_1000";


    @Inject
    private Path sshdPath;

    @Autowired
    SkanmotreferansenrProperties properties;

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        try {
            preparePath(inngaaende);
        } catch (Exception e) {
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        } try {
            preparePath(processed);
        } catch (Exception e) {
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        }
        try {
            preparePath(feilmappe);
        } catch (Exception e) {
            //noop. Windows sliter med å slette filene, de blir kun satt til "unavailable"
        }
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void shouldBehandleEncryptedZip() throws IOException {
        // 09.06.2020_R123456780_1_2000.zip
        // OK   - 09.06.2020_R123456780_0001
        // OK   - 09.06.2020_R123456780_0002 (mangler førstesidemetadata, Oppretter journalpost med tema UKJ)
        // FEIL - 09.06.2020_R123456780_0003 (valideringsfeil, mangler referansenr)
        // FEIL - 09.06.2020_R123456780_0004 (mangler xml)
        // FEIL - 09.06.2020_R123456780_0005 (mangler pdf)
        copyFileFromClasspathToInngaaende(ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");
        setUpHappyStubs();

        await().atMost(ofSeconds(15)).untilAsserted(() -> {
            try {
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE)
                        .resolve(ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION))
                        .collect(Collectors.toList())).hasSize(3);
            } catch (NoSuchFileException e) {
                fail();
            }
        });

        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertThat(feilmappeContents).containsExactlyInAnyOrder(
                "09.06.2020_R123456780_0003.zip",
                "09.06.2020_R123456780_0004.zip",
                "09.06.2020_R123456780_0005.zip");
        verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1)));
        verify(exactly(1), getRequestedFor(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND)));
        verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
        verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)));

    }

    @Test
    public void shouldFailWithUnEncryptedDotEncDotZipExtension() throws IOException {
        //ZipException: En .enc-file kom inn men filene er ukrypterte
        //should be sent to feilmappe
        copyFileFromClasspathToInngaaende(UNENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");
        setUpHappyStubs();

        await().atMost(ofSeconds(15)).untilAsserted(() -> {
            try {
                final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                        .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                        .collect(Collectors.toList());
                assertTrue(feilmappeContents.contains(UNENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip"));
            } catch (NoSuchFileException e) {
                fail();
            }
        });

    }

    @Test
    public void shouldMoveZipToFeilomraadeWhenBadEncryption() throws IOException {
        //ZipException: Filene er ikke kryptert med AES men en annen krypteringsmetode
        //should be sent to feilmappe
        copyFileFromClasspathToInngaaende(FAIL_ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");

        await().atMost(ofSeconds(15)).untilAsserted(() -> {
            try {
                final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                        .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                        .collect(Collectors.toList());
                assertTrue(feilmappeContents.contains(FAIL_ENCRYPTED_ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip"));
            } catch (NoSuchFileException e) {
                fail();
            }
        });

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
        stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())));
    }

    private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
        Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
    }

}