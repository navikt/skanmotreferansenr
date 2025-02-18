package no.nav.skanmotreferansenr.avstem;

import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotreferansenr.MdcSetterProcessor;
import no.nav.skanmotreferansenr.RemoveMdcProcessor;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotreferansenr.jira.OpprettJiraService.ANTALL_FILER_AVSTEMT;
import static no.nav.skanmotreferansenr.jira.OpprettJiraService.ANTALL_FILER_FEILET;
import static no.nav.skanmotreferansenr.jira.OpprettJiraService.avstemmingsfilDato;
import static no.nav.skanmotreferansenr.jira.OpprettJiraService.parseDatoFraFilnavn;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_AVSTEMMINGSFIL_NAVN;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_AVSTEMT_DATO;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 1500;
	private final AvstemService avstemService;
	private final OpprettJiraService opprettJiraService;

	public AvstemRoute(AvstemService avstemService,
					   OpprettJiraService opprettJiraService) {
		this.avstemService = avstemService;
		this.opprettJiraService = opprettJiraService;
	}

	@Override
	public void configure() {

		// @formatter:off
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.log(WARN, log, "Skanmotreferansenr feilet teknisk, Exception:${exception}");

		onException(AbstractSkanmotreferansenrFunctionalException.class, JiraClientException.class)
				.handled(true)
				.useOriginalMessage()
				.log(WARN, log, "Skanmotreferansenr feilet å prossessere avstemmingsfil. Exception:${exception};" );

		onException(GenericFileOperationFailedException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotreferansenr fant ikke avstemmingsfil for ${exchangeProperty." + MDC_AVSTEMT_DATO + "}. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");


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
						.process(exchange -> exchange.setProperty(MDC_AVSTEMT_DATO, avstemmingsfilDato()))
						.log(ERROR, log, "Skanmotreferansenr fant ikke avstemmingsfil for ${exchangeProperty." + MDC_AVSTEMT_DATO + "}. Undersøk tilfellet og evt. ser opprettet Jira-sak.")
						.bean(opprettJiraService)
						.log(INFO, log, "Skanmotreferansenr opprettet jira-sak med key=${body.jiraIssueKey} for manglende avstemmingsfil.")
				.otherwise()
					.log(INFO, log, "Skanmotreferansenr starter behandling av avstemmingsfil=${file:name}.")
					.process(exchange -> {
						exchange.setProperty(MDC_AVSTEMMINGSFIL_NAVN, simple("${file:name}"));
						exchange.setProperty(MDC_AVSTEMT_DATO,  parseDatoFraFilnavn(exchange));
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
