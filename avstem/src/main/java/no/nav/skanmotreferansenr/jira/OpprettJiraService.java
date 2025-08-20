package no.nav.skanmotreferansenr.jira;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.DayOfWeek.MONDAY;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.apache.camel.Exchange.FILE_NAME_ONLY;

@Slf4j
@Component
public class OpprettJiraService {

	private static final String DESCRIPTION = "Se vedlegg for en oversikt over manglende avstemmingsreferanser for skannede dokumenter fra skanmotreferansenr \n";
	public static final String SUMMARY = "Skanmotreferansenr: Manglende avstemmingsreferanser for skannede dokumenter";
	private static final String JIRA_BRUKER_NAVN = "srvjiradokdistavstemming";
	public static final String ANTALL_FILER_AVSTEMT = "Antall filer avstemt";
	public static final String ANTALL_FILER_FEILET = "Antall filer feilet";
	private static final List<String> LABEL = List.of("Skanmotreferansenr_avvik");

	private final JiraService jiraService;

	public OpprettJiraService(JiraService jiraService) {
		this.jiraService = jiraService;
	}

	@Handler
	public JiraResponse opprettAvstemJiraOppgave(byte[] csvByte, Exchange exchange) {
		LocalDate avstemmingsfilDato = exchange.getProperty(EXCHANGE_AVSTEMT_DATO, LocalDate.class);
		try {
			if (csvByte == null) {
				return opprettJiraForManglendeAvstemmingsfil(avstemmingsfilDato);
			}

			Integer antallAvstemt = exchange.getProperty(ANTALL_FILER_AVSTEMT, Integer.class);
			Integer antallFeilet = exchange.getProperty(ANTALL_FILER_FEILET, Integer.class);
			JiraRequest jiraRequest = mapJiraRequest(csvByte, antallAvstemt, antallFeilet, avstemmingsfilDato);

			return jiraService.opprettJiraMMAOppgaveMedVedlegg(jiraRequest);
		} catch (JiraClientException e) {
			throw new SkanmotreferansenrFunctionalException("kunne ikke opprette jira oppgave", e);
		}
	}

	private JiraResponse opprettJiraForManglendeAvstemmingsfil(LocalDate avstemmingsfilDato) {
		return jiraService.opprettJiraMMAOppgave(JiraRequest.builder()
				.summary("Skanmotreferansenr: Avstemmingfil mangler for " + avstemmingsfilDato)
				.description("Skanmotreferansenr fant ikke avstemmingsfil for " + avstemmingsfilDato + ". Undersøk tilfellet og evt. kontakt Iron Mountain.")
				.reporterName(JIRA_BRUKER_NAVN)
				.labels(LABEL)
				.build());
	}

	private JiraRequest mapJiraRequest(byte[] csvByte, int antallAvstemt, int antallFeilet, LocalDate avstemmingsfilDato) {
		return JiraRequest.builder()
				.summary(SUMMARY)
				.description(prettifySummary(DESCRIPTION, antallAvstemt, antallFeilet))
				.reporterName(JIRA_BRUKER_NAVN)
				.labels(LABEL)
				.vedlegg(csvByte)
				.avstemmingsfilDato(avstemmingsfilDato)
				.build();
	}

	public static String prettifySummary(String melding, int antallAvstemt, int antallFeilet) {
		var builder = new StringBuilder();
		return builder.append(melding)
				.append("\nAntall filer avstemt: ").append(antallAvstemt)
				.append("\nAntall filer funnet: ").append(antallAvstemt - antallFeilet)
				.append("\nAntall filer feilet: ").append(antallFeilet).toString();

	}

	/**
	 * finnForrigeVirkedag genererer datoen for forrige virkedag, som kan brukes i loggen og til å opprette Jira-saken.
	 */
	public static LocalDate finnForrigeVirkedag(Clock clock) {
		LocalDate todaysDate = LocalDate.now(clock);
		return MONDAY.equals(todaysDate.getDayOfWeek()) ? todaysDate.minusDays(3) :
				todaysDate.minusDays(1);
	}

	public static LocalDate parseDatoFraFilnavn(Exchange exchange) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		return LocalDate.parse(exchange.getIn().getHeader(FILE_NAME_ONLY, String.class).substring(0, 10), formatter);
	}
}
