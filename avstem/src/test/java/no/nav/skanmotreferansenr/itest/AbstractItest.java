package no.nav.skanmotreferansenr.itest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = AvstemTestConfig.class,
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public abstract class AbstractItest {

	public static final String URL_DOKARKIV_AVSTEMREFERANSER = "/rest/journalpostapi/v1/avstemReferanser";
	public static final String JIRA_OPPRETTE_URL = "/rest/api/2/issue";
	public static final String JIRA_VEDLEGG_URL = "/rest/api/2/issue/MMA-134/attachments";
	public static final String JIRA_PROJECT_URL = "/rest/api/2/project/MMA";


	public void setUpStubs() {
		stubAzureToken();
		stubJiraHentProject();
		stubJiraPostVedleggDokument();
		jiraHappyUpdateSak();
		jiraHappyGetIssue();
	}

	public void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	public static void stubPostAvstemJournalpost(String path) {
		stubFor(post(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withHeader("Connection", "close")
						.withBodyFile(path)));
	}

	public static void stubJiraOpprettOppgave() {
		stubFor(post(urlMatching(JIRA_OPPRETTE_URL))
				.willReturn(aResponse().withStatus(CREATED.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("jira/jiraresponse.json")));
	}

	public static void jiraHappyGetIssue() {
		stubFor(get(urlMatching(JIRA_OPPRETTE_URL + "/MMA-134"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("jira/jiraresponse.json")));
	}

	public static void jiraHappyUpdateSak() {
		stubFor(post(urlMatching(JIRA_OPPRETTE_URL + "/MMA-134/transitions"))
				.willReturn(aResponse().withStatus(HttpStatus.NO_CONTENT.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	public static void stubJiraHentProject() {
		stubFor(get(urlMatching(JIRA_PROJECT_URL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("jira/project.json")));
	}

	public static void stubJiraPostVedleggDokument() {
		stubFor(post(urlEqualTo(JIRA_VEDLEGG_URL))
				.willReturn(aResponse().withStatus(NO_CONTENT.value())));
	}

	public static void stubBadRequestJiraOpprettOppgave() {
		stubFor(post(urlMatching(JIRA_OPPRETTE_URL))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("jira/jiraresponse.json")));
	}
}
