package no.nav.skanmotreferansenr.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = {TestConfig.class},
		webEnvironment = RANDOM_PORT,
		properties = "spring.cloud.vault.token=123456"
)
@AutoConfigureWireMock(port = 0)
@ImportAutoConfiguration
@ActiveProfiles("itest")
public class AbstractItest {

	private final String URL_STS = "/rest/v1/sts/token";

	String URL_FOERSTESIDEGENERATOR_OK_1 = "/api/foerstesidegenerator/v1/foersteside/1111111111111";
	String URL_FOERSTESIDEGENERATOR_NOT_FOUND = "/api/foerstesidegenerator/v1/foersteside/2222222222222";

	private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
	String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
	String URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG = "/rest/journalpostapi/v1/dokumentInfo/[0-9]+/logiskVedlegg/";

	String LOGISK_VEDLEGG_ID = "885522";
	final String LOEPENR_OK = "1111111111111";

	@SneakyThrows
	@BeforeEach
	void setUp() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();

		stubFor(post(urlMatching(URL_STS))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/sts/happy_sts_response.json"))));

		this.stubForDokArkiv();
		this.stubForFoersteSideGenerator();
	}

	public void stubForFoersteSideGenerator() {
		stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND))
				.willReturn(aResponse()
						.withStatus(NOT_FOUND.value())));

		stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("foersteside/foersteside_HAPPY.json")));

	}

	public void stubForDokArkiv() throws IOException {
		stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("journalpost/opprett_journalpost_response_HAPPY.json")));

		stubFor(post(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/journalpost/opprett_journalpost_response_HAPPY.json"))));

		stubFor(post(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withJsonBody(Json.node(
								"{\"logiskVedleggId\": \"" + LOGISK_VEDLEGG_ID + "\"}"
						))));
	}

	private static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
	}
}
