package no.nav.skanmotreferansenr.decrypt;

import lombok.extern.slf4j.Slf4j;
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
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.skanmotreferansenr.metrics.DokCounter.DOMAIN;
import static no.nav.skanmotreferansenr.metrics.DokCounter.REFERANSENR;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.FILE_NAME_PRODUCED;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Slf4j
@Component
public class PostboksReferansenrRoutePGPEncrypted extends RouteBuilder {
	public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
	public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
	public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
	public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
	static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

	private final SkanmotreferansenrProperties skanmotreferansenrProperties;
	private final PostboksReferansenrService postboksReferansenrService;
	private final PgpDecryptService pgpDecryptService;

	@Autowired
	public PostboksReferansenrRoutePGPEncrypted(
			SkanmotreferansenrProperties skanmotreferansenrProperties,
			PostboksReferansenrService postboksReferansenrService,
			PgpDecryptService pgpDecryptService) {
		this.skanmotreferansenrProperties = skanmotreferansenrProperties;
		this.postboksReferansenrService = postboksReferansenrService;
		this.pgpDecryptService = pgpDecryptService;
	}

	@Override
	public void configure() {
		String PGP_AVVIK = "direct:pgp_encrypted_avvik_referansenr";
		String PROCESS_PGP_ENCRYPTED = "direct:pgp_encrypted_process_referansenr";


		// @formatter:off
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(ERROR, log, "Skanmotreferansenr feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
				.to(PGP_AVVIK)
				.log(ERROR, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");


		// Får ikke dekryptert .zip.pgp - mest sannsynlig mismatch mellom private key og public key
		onException(PGPException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(ERROR, log, "Skanmotreferansenr feilet i dekryptering av .zip.pgp for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip.pgp"))
				.to("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.feilmappe}}" +
						"?{{skanmotreferansenr.endpointconfig}}")
				.log(ERROR, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
				.end()
				.process(new MdcRemoverProcessor());

		// Kjente funksjonelle feil
		onException(AbstractSkanmotreferansenrFunctionalException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(WARN, log, "Skanmotreferansenr feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
				.to(PGP_AVVIK)
				.log(WARN, log, "Skanmotreferansenr skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

		from("{{skanmotreferansenr.endpointuri}}/{{skanmotreferansenr.filomraade.inngaaendemappe}}" +
				"?{{skanmotreferansenr.endpointconfig}}" +
				"&delay=" + TimeUnit.SECONDS.toMillis(60) +
				"&antInclude=*zip.pgp,*ZIP.pgp" +
				"&initialDelay=1000" +
				"&maxMessagesPerPoll=10" +
				"&move=processed" +
				"&scheduler=spring&scheduler.cron={{skanmotreferansenr.schedule}}")
				.routeId("read_encrypted_PGP_referansenr_zip_from_sftp")
				.log(INFO, log, "Skanmotreferansenr-pgp starter behandling av fil=${file:absolute.path}.")
				.setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
				.process(exchange -> exchange.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, cleanDotPgpExtension(simple("${file:name.noext.single}"), exchange)))
				.process(new MdcSetterProcessor())
				.bean(pgpDecryptService)
				.split(new ZipSplitter()).streaming()
					.aggregate(simple("${file:name.noext.single}"), new PostboksReferansenrSkanningAggregator())
						.completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
						.completionTimeout(skanmotreferansenrProperties.getCompletiontimeout().toMillis())
						.setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
						.process(exchange -> {

						})
						.process(new MdcSetterProcessor())
						.process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DOMAIN, REFERANSENR)))
						.process(exchange -> exchange.getIn().getBody(PostboksReferansenrEnvelope.class).validate())
						.bean(new SkanningmetadataUnmarshaller())
						.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
						.to(PROCESS_PGP_ENCRYPTED)
					.end() // aggregate
				.end() // split
				.process(new MdcRemoverProcessor())
				.log(INFO, log, "Skanmotreferansenr behandlet ferdig fil=${file:absolute.path}.");

		from(PROCESS_PGP_ENCRYPTED)
				.routeId(PROCESS_PGP_ENCRYPTED)
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotreferansenr behandler " + KEY_LOGGING_INFO + ".")
				.bean(postboksReferansenrService)
				.log(INFO, log, "Skanmotreferansenr journalførte journalpostId=${body}. " + KEY_LOGGING_INFO + ".")
				.process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DOMAIN, REFERANSENR)))
				.process(new MdcRemoverProcessor());

		from(PGP_AVVIK)
				.routeId("pgp_encrypted_avvik_referansenr")
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

	// Input blir .zip siden .pgp er strippet bort
	private String cleanDotPgpExtension(ValueBuilder value1, Exchange exchange) {
		String stringRepresentation = value1.evaluate(exchange, String.class);
		if (stringRepresentation.contains(".zip")) {
			return stringRepresentation.replace(".zip", "");
		}
		return stringRepresentation;
	}
}
