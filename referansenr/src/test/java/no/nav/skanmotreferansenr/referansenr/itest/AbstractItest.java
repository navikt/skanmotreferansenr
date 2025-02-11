package no.nav.skanmotreferansenr.referansenr.itest;

import com.github.tomakehurst.wiremock.common.Json;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = TestConfig.class,
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class AbstractItest {

	public static final String URL_FOERSTESIDEGENERATOR_OK_1 = "/api/foerstesidegenerator/v1/foersteside/1111111111111";
	public static final String URL_FOERSTESIDEGENERATOR_NOT_FOUND = "/api/foerstesidegenerator/v1/foersteside/2222222222222";

	public static final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
	public static final String URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG = "/rest/journalpostapi/v1/dokumentInfo/[0-9]+/logiskVedlegg/";

	final String LOGISK_VEDLEGG_ID = "885522";
	final String LOEPENR_OK = "1111111111111";


	public void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	public void setUpStubs() {

		stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)).willReturn(aResponse()
				.withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withHeader("Connection", "close")
				.withBodyFile("journalpost/opprett_journalpost_response_HAPPY.json"))
		);

		stubFor(post(urlMatching(URL_DOKARKIV_DOKUMENTINFO_LOGISKVEDLEGG)).willReturn(aResponse()
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withHeader("Connection", "close")
				.withJsonBody(Json.node(
						"{\"logiskVedleggId\": \"" + LOGISK_VEDLEGG_ID + "\"}"
				)))
		);

		stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_NOT_FOUND)).willReturn(aResponse()
				.withHeader("Connection", "close")
				.withStatus(NOT_FOUND.value()))
		);

		stubFor(get(urlMatching(URL_FOERSTESIDEGENERATOR_OK_1)).willReturn(aResponse()
				.withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withHeader("Connection", "close")
				.withBodyFile("foersteside/foersteside_HAPPY.json"))
		);
	}

	public void stubOpprettJournalpostResponseConflictWithValidResponse() {
		stubFor(post("/rest/journalpostapi/v1/journalpost?foersoekFerdigstill=false").willReturn(aResponse()
				.withStatus(CONFLICT.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withHeader("Connection", "close")
				.withBody(classpathToString("__files/journalpost/allerede_opprett_journalpost_response_HAPPY.json")))
		);
	}

	protected void stubOpprettJournalpostResponseConflictWithInvalidResponse() {
		stubFor(post("/rest/journalpostapi/v1/journalpost?foersoekFerdigstill=false").willReturn(aResponse()
				.withStatus(CONFLICT.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withHeader("Connection", "close"))
		);
	}

	@SneakyThrows
	private static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
	}
}
