package no.nav.skanmotreferansenr;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.FILE_NAME_PRODUCED;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Slf4j
@Component
public class PostboksReferansenrRoute extends RouteBuilder {
	public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
	public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
	public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
	public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
	static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

	private final SkanmotreferansenrProperties skanmotreferansenrProperties;
	private final PostboksReferansenrService postboksReferansenrService;
	private final ErrorMetricsProcessor errorMetricsProcessor;

	@Autowired
	public PostboksReferansenrRoute(SkanmotreferansenrProperties skanmotreferansenrProperties, PostboksReferansenrService postboksReferansenrService) {
		this.skanmotreferansenrProperties = skanmotreferansenrProperties;
		this.postboksReferansenrService = postboksReferansenrService;
		this.errorMetricsProcessor = new ErrorMetricsProcessor();
	}

	@Override
	public void configure() {

		// @formatter:off
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(errorMetricsProcessor)
				.log(ERROR, log, "Skanmotreferansenr feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}. ${exception.stacktrace}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
				.to("direct:avvik")
				.log(ERROR, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

		// Kjente funksjonelle feil
		onException(AbstractSkanmotreferansenrFunctionalException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(errorMetricsProcessor)
				.log(WARN, log, "Skanmotreferansenr feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
				.to("direct:avvik")
				.log(WARN, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

		from("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.inngaaendemappe}}" +
				"?{{skanmotreferansenr.endpointconfig}}" +
				"&delay=" + TimeUnit.SECONDS.toMillis(60) +
				"&antExclude=*enc.zip, *enc.ZIP" +
				"&antExclude=*zip.pgp, *ZIP.pgp" +
				"&antInclude=*.zip,*.ZIP" +
				"&initialDelay=1000" +
				"&maxMessagesPerPoll=10" +
				"&move=processed" +
				"&scheduler=spring&scheduler.cron={{skanmotreferansenr.schedule}}")
				.routeId("read_zip_from_sftp")
				.log(INFO, log, "Skanmotreferansenr starter behandling av fil=${file:absolute.path}.")
				.setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
				.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${file:name.noext.single}"))
				.process(new MdcSetterProcessor())
				.split(new ZipSplitter()).streaming()
					.aggregate(simple("${file:name.noext.single}"), new PostboksReferansenrSkanningAggregator())
						.completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
						.completionTimeout(skanmotreferansenrProperties.getCompletiontimeout().toMillis())
						.setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
						.process(new MdcSetterProcessor())
						.process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DokCounter.DOMAIN, DokCounter.REFERANSENR)))
						.process(exchange -> exchange.getIn().getBody(PostboksReferansenrEnvelope.class).validate())
						.bean(new SkanningmetadataUnmarshaller())
						.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
						.to("direct:process_referansenr")
					.end() // aggregate
				.end() // split
				.process(new MdcRemoverProcessor())
				.log(INFO, log, "Skanmotreferansenr behandlet ferdig fil=${file:absolute.path}.");

		from("direct:process_referansenr")
				.routeId("process_referansenr")
				.process(new MdcSetterProcessor())
				.bean(postboksReferansenrService)
				.process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DokCounter.DOMAIN, DokCounter.REFERANSENR)))
				.process(new MdcRemoverProcessor());

		from("direct:avvik")
				.routeId("avvik")
				.choice().when(body().isInstanceOf(PostboksReferansenrEnvelope.class))
				.setBody(simple("${body.createZip}"))
				.to("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.feilmappe}}" +
						"?{{skanmotreferansenr.endpointconfig}}")
				.otherwise()
				.log(ERROR, log, "Skanmotreferansenr teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
				.end()
				.process(new MdcRemoverProcessor());

		// @formatter:on
	}
}