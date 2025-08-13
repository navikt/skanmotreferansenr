package no.nav.skanmotreferansenr.avstem;

import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotreferansenr.MdcSetterProcessor;
import no.nav.skanmotreferansenr.RemoveMdcProcessor;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.jira.OpprettJiraService;
import no.nav.skanmotreferansenr.util.Helligdager;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;

import static no.nav.skanmotreferansenr.jira.OpprettJiraService.ANTALL_FILER_AVSTEMT;
import static no.nav.skanmotreferansenr.jira.OpprettJiraService.ANTALL_FILER_FEILET;
import static no.nav.skanmotreferansenr.jira.OpprettJiraService.finnForrigeVirkedag;
import static no.nav.skanmotreferansenr.jira.OpprettJiraService.parseDatoFraFilnavn;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.EXCHANGE_AVSTEMMINGSFIL_NAVN;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.EXCHANGE_AVSTEMT_DATO;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 15000;
	private final AvstemService avstemService;
	private final OpprettJiraService opprettJiraService;
	private final Clock clock;

	public AvstemRoute(AvstemService avstemService,
					   OpprettJiraService opprettJiraService,
					   Clock clock
	) {
		this.avstemService = avstemService;
		this.opprettJiraService = opprettJiraService;
		this.clock = clock;
	}

	@Override
	public void configure() {

		// @formatter:off
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.logStackTrace(true)
				.log(WARN, log, "Skanmotreferansenr feilet teknisk, Exception:${exception}");

		onException(AbstractSkanmotreferansenrFunctionalException.class, JiraClientException.class)
				.handled(true)
				.useOriginalMessage()
				.logStackTrace(true)
				.log(WARN, log, "Skanmotreferansenr feilet å prossessere avstemmingsfil. Exception:${exception};" );

		onException(GenericFileOperationFailedException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotreferansenr fant ikke avstemmingsfil for ${exchangeProperty." + EXCHANGE_AVSTEMT_DATO + "}. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");


		from("cron:tab?schedule={{skanmotreferansenr.avstem.schedule}}")
				.pollEnrich("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.avstemmappe}}" +
						"?{{skanmotreferansenr.endpointconfig}}" +
						"&antInclude=*.txt,*.TXT" +
						"&move=processed", CONNECTION_TIMEOUT)
				.routeId("avstem_routeid")
				.autoStartup("{{skanmotreferansenr.avstem.startup}}")
				.log(INFO, log, "Skanmotreferansenr starter cron jobb for å avstemme referanser...")
				.process(new MdcSetterProcessor())
				.choice()
					.when(header(FILE_NAME).isNull())
						.process(exchange -> exchange.setProperty(EXCHANGE_AVSTEMT_DATO, finnForrigeVirkedag(clock)))
						.log(ERROR, log, "Skanmotreferansenr fant ikke avstemmingsfil for ${exchangeProperty." + EXCHANGE_AVSTEMT_DATO + "}. Undersøk tilfellet og evt. ser opprettet Jira-sak.")
						.process(exchange -> {
							if (Helligdager.erHelligdag(finnForrigeVirkedag(clock))) {
								log.warn("I går var det helligdag, Da kommer det normalt sett ikke avstemmingsfiler");
							}
							else {
								JiraResponse jiraResponse = opprettJiraService.opprettAvstemJiraOppgave(exchange.getIn().getBody(byte[].class), exchange);
								log.info("Skanmotreferansenr opprettet jira-sak med key={} for manglende avstemmingsfil.", jiraResponse.jiraIssueKey());
							}
						})

				//TODO: rydd opp her. Sørg for at testene tar høyde for at det kan ha vært helligdag i går (mock localDate.now() til faste datoer.
				//TODO: skriv tester som mocker helligdager
//						.choice()
				.otherwise()
					.log(INFO, log, "Skanmotreferansenr starter behandling av avstemmingsfil=${file:name}.")
					.process(exchange -> {
						exchange.setProperty(EXCHANGE_AVSTEMMINGSFIL_NAVN, simple("${file:name}"));
						exchange.setProperty(EXCHANGE_AVSTEMT_DATO,  parseDatoFraFilnavn(exchange));
					})
					.split(body().tokenize())
					.streaming()
						.aggregationStrategy(new AvstemAggregationStrategy())
						.convertBodyTo(Set.class)
					.end()
					.process(exchange -> {
						Set<String> avstemmingsReferanser = exchange.getIn().getBody(Set.class);
						exchange.getIn().setBody(avstemmingsReferanser);
					})
					.setProperty(ANTALL_FILER_AVSTEMT, simple("${body.size}"))
					.log(INFO, log, "hentet ${body.size} avstemmingReferanser fra sftp server")
					.bean(avstemService)
					.choice()
						.when(simple("${body}").isNotNull())
							.setProperty(ANTALL_FILER_FEILET, simple("${body.size}"))
							.log(INFO, log, "Skanmotreferansenr fant ${body.size} feilende avstemmingsreferanser")
							.marshal().csv()
							.bean(opprettJiraService)
							.log(INFO, log, "Har opprettet Jira-sak=${body.jiraIssueKey} for feilende skanmotreferansenr avstemmingsreferanser")
							.process(new RemoveMdcProcessor())
					.endChoice()
				.endChoice()
				.end()
				.log(INFO, log, "Skanmotreferansenr behandlet ferdig avstemmingsfil: ${file:name}");
		// @formatter:on
	}
}
