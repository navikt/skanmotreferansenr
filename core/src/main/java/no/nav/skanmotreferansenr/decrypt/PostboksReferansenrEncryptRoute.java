package no.nav.skanmotreferansenr.decrypt;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import no.nav.skanmotreferansenr.ErrorMetricsProcessor;
import no.nav.skanmotreferansenr.MdcRemoverProcessor;
import no.nav.skanmotreferansenr.MdcSetterProcessor;
import no.nav.skanmotreferansenr.PostboksReferansenrEnvelope;
import no.nav.skanmotreferansenr.PostboksReferansenrService;
import no.nav.skanmotreferansenr.PostboksReferansenrSkanningAggregator;
import no.nav.skanmotreferansenr.SkanningmetadataUnmarshaller;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.SimpleBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.skanmotreferansenr.metrics.DokCounter.DOMAIN;
import static no.nav.skanmotreferansenr.metrics.DokCounter.REFERANSENR;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.FILE_NAME_PRODUCED;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;


@Slf4j
@Component
public class PostboksReferansenrEncryptRoute extends RouteBuilder {
	public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseEncryptedZipname";
	public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseEncryptedBatchNavn";
	public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseEncryptedFileBasename";
	public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
	static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

	private final SkanmotreferansenrProperties skanmotreferansenrProperties;
	private final PostboksReferansenrService postboksReferansenrService;
	private final ErrorMetricsProcessor errorMetricsProcessor;
	private final String passphrase;

	@Inject
	public PostboksReferansenrEncryptRoute(
			@Value("${skanmotreferansenr.secret.passphrase}") String passphrase,
			SkanmotreferansenrProperties skanmotreferansenrProperties,
			PostboksReferansenrService postboksReferansenrService
	) {
		this.skanmotreferansenrProperties = skanmotreferansenrProperties;
		this.postboksReferansenrService = postboksReferansenrService;
		this.errorMetricsProcessor = new ErrorMetricsProcessor();
		this.passphrase = passphrase;
	}

	@Override
	public void configure() {
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(errorMetricsProcessor)
				.log(ERROR, log, "Skanmotreferansenr feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}. ${exception.stacktrace}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
				.to("direct:encrypted_avvik")
				.log(ERROR, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

		onException(ZipException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(WARN, log, "Feil passord for en fil " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.enc.zip"))
				.to("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.feilmappe}}" +
						"?{{skanmotreferansenr.endpointconfig}}")
				.log(WARN, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
				.end()
				.process(new MdcRemoverProcessor());

		// Kjente funksjonelle feil
		onException(AbstractSkanmotreferansenrFunctionalException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(errorMetricsProcessor)
				.log(WARN, log, "Skanmotreferansenr feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
				.to("direct:encrypted_avvik")
				.log(WARN, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

		from("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.inngaaendemappe}}" +
				"?{{skanmotreferansenr.endpointconfig}}" +
				"&delay=" + SECONDS.toMillis(60) +
				"&antInclude=*enc.zip,*enc.ZIP" +
				"&initialDelay=1000" +
				"&maxMessagesPerPoll=10" +
				"&move=processed" +
				"&scheduler=spring&scheduler.cron={{skanmotreferansenr.schedule}}")
				.routeId("read_encrypted_zip_from_sftp")
				.log(INFO, log, "Skanmotreferansenr starter behandling av fil=${file:absolute.path}.")
				.setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
				.process(exchange -> exchange.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, cleanDotEncExtension(simple("${file:name.noext.single}"), exchange)))
				.process(new MdcSetterProcessor())
				.split(new ZipSplitterEncrypted(passphrase)).streaming()
				.aggregate(simple("${file:name.noext.single}"), new PostboksReferansenrSkanningAggregator())
				.completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
				.completionTimeout(skanmotreferansenrProperties.getCompletiontimeout().toMillis())
				.setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
				.process(new MdcSetterProcessor())
				.process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DOMAIN, REFERANSENR)))
				.process(exchange -> exchange.getIn().getBody(PostboksReferansenrEnvelope.class).validate())
				.bean(new SkanningmetadataUnmarshaller())
				.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
				.to("direct:encrypted_process_referansenr")
				.end() // aggregate
				.end() // split
				.process(new MdcRemoverProcessor())
				.log(INFO, log, "Skanmotreferansenr behandlet ferdig fil=${file:absolute.path}.");

		from("direct:encrypted_process_referansenr")
				.routeId("encrypted_process_referansenr")
				.process(new MdcSetterProcessor())
				.bean(postboksReferansenrService)
				.process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DOMAIN, REFERANSENR)))
				.process(new MdcRemoverProcessor());

		from("direct:encrypted_avvik")
				.routeId("encrypted_avvik")
				.choice().when(body().isInstanceOf(PostboksReferansenrEnvelope.class))
				.setBody(simple("${body.createZip}"))
				.to("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.feilmappe}}" +
						"?{{skanmotreferansenr.endpointconfig}}")
				.otherwise()
				.log(ERROR, log, "Skanmotreferansenr teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
				.end()
				.process(new MdcRemoverProcessor());
	}

	private String cleanDotEncExtension(SimpleBuilder value1, Exchange exchange) {
		String stringRepresentation = value1.evaluate(exchange, String.class);
		if (stringRepresentation.contains(".enc")) {
			return stringRepresentation.replace(".enc", "");
		}
		return stringRepresentation;
	}
}
