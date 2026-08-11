package no.nav.skanmotreferansenr.mapper;

import no.nav.skanmotreferansenr.consumer.foersteside.data.Avsender;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.AvsenderMottaker;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Bruker;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Dokument;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.DokumentVariant;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Tilleggsopplysning;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class OpprettJournalpostRequestMapper {
	private static final String REFERANSENR = "referansenr";
	private static final String ENDORSERNR = "endorsernr";
	private static final String FYSISKPOSTBOKS = "fysiskPostboks";
	private static final String STREKKODEPOSTBOKS = "strekkodePostboks";
	private static final String ANTALL_SIDER = "antallSider";


	// joark domenet
	private static final String JOURNALPOSTTYPE = "INNGAAENDE";
	private static final String FILTYPE_PDFA = "PDFA";
	private static final String FILTYPE_XML = "XML";
	private static final String FIL_EXTENSION_PDF = "pdf";
	private static final String FIL_EXTENSION_XML = "xml";
	private static final String VARIANTFORMAT_PDF = "ARKIV";
	private static final String VARIANTFORMAT_XML = "SKANNING_META";
	private static final String DOKUMENTKATEGORI_IS = "IS";
	private static final String TEMA_UKJENT = "UKJ";
	static final String AVSENDER_IDTYPE_PERSON = "FNR";
	static final String AVSENDER_IDTYPE_ORGANISASJON = "ORGNR";

	static final String DOUBLE_ZERO_PADDING = "00";
	private static final int FOLKEREGISTER_IDENT_LENGTH = 11;
	private static final int NORSK_ORGANISASJONSNUMMER_LENGTH = 9;

	public static OpprettJournalpostRequest mapMetadataToOpprettJournalpostRequest(Skanningmetadata skanningmetadata, FoerstesideMetadata foerstesideMetadata, Filepair filepair, Bruker bruker) {
		Journalpost journalpost = skanningmetadata.getJournalpost();
		SkanningInfo skanningInfo = skanningmetadata.getSkanningInfo();

		String tema = extractTema(foerstesideMetadata);
		String behandlingstema = foerstesideMetadata.getBehandlingstema();
		String tittel = foerstesideMetadata.getArkivtittel();
		String kanal = skanningmetadata.getJournalpost().getMottakskanal();
		String journalfoerendeEnhet = foerstesideMetadata.getEnhetsnummer();
		String eksternReferanseId = appendFileType(filepair.getName(), FIL_EXTENSION_PDF);
		Date datoMottatt = skanningmetadata.getJournalpost().getDatoMottatt();

		AvsenderMottaker avsenderMottaker = extractAvsenderMottaker(foerstesideMetadata);

		List<Tilleggsopplysning> tilleggsopplysninger = Stream.of(
						new Tilleggsopplysning(REFERANSENR, journalpost.getReferansenummer()),
						new Tilleggsopplysning(ENDORSERNR, journalpost.getEndorsernr()),
						new Tilleggsopplysning(FYSISKPOSTBOKS, skanningInfo.getFysiskPostboks()),
						new Tilleggsopplysning(STREKKODEPOSTBOKS, skanningInfo.getStrekkodePostboks()),
						new Tilleggsopplysning(ANTALL_SIDER, journalpost.getAntallSider()))
				.filter(tilleggsopplysning -> notNullOrEmpty(tilleggsopplysning.getVerdi()))
				.toList();

		DokumentVariant pdf = DokumentVariant.builder()
				.filtype(FILTYPE_PDFA)
				.variantformat(VARIANTFORMAT_PDF)
				.fysiskDokument(filepair.getPdf())
				.filnavn(appendFileType(filepair.getName(), FIL_EXTENSION_PDF))
				.batchnavn(journalpost.getBatchnavn())
				.build();

		DokumentVariant xml = DokumentVariant.builder()
				.filtype(FILTYPE_XML)
				.variantformat(VARIANTFORMAT_XML)
				.fysiskDokument(filepair.getXml())
				.filnavn(appendFileType(filepair.getName(), FIL_EXTENSION_XML))
				.batchnavn(journalpost.getBatchnavn())
				.build();

		Dokument dokument = Dokument.builder()
				.tittel(foerstesideMetadata.getArkivtittel())
				.brevkode(foerstesideMetadata.getNavSkjemaId())
				.dokumentKategori(DOKUMENTKATEGORI_IS)
				.dokumentVarianter(List.of(pdf, xml))
				.build();

		return OpprettJournalpostRequest.builder()
				.journalpostType(JOURNALPOSTTYPE)
				.avsenderMottaker(avsenderMottaker)
				.bruker(bruker)
				.tema(tema)
				.behandlingstema(behandlingstema)
				.tittel(tittel)
				.kanal(kanal)
				.journalfoerendeEnhet(journalfoerendeEnhet)
				.eksternReferanseId(eksternReferanseId)
				.datoMottatt(datoMottatt)
				.tilleggsopplysninger(tilleggsopplysninger)
				.dokumenter(List.of(dokument))
				.build();
	}

	private static String extractTema(FoerstesideMetadata foerstesideMetadata) {
		if (foerstesideMetadata.getTema() == null) {
			return TEMA_UKJENT;
		}
		return foerstesideMetadata.getTema();
	}

	private static AvsenderMottaker extractAvsenderMottaker(FoerstesideMetadata foerstesideMetadata) {
		Avsender avsender = foerstesideMetadata.getAvsender();
		if (avsender == null) {
			return null;
		}
		return AvsenderMottaker.builder()
				.id(mapAvsenderId(avsender.getAvsenderId()))
				.idType(mapAvsenderMottakerIdType(avsender.getAvsenderId()))
				.navn(avsender.getAvsenderNavn())
				.build();
	}

	private static String mapAvsenderId(String avsenderId) {
		if (isDoubleZeroPrefixed(avsenderId)
			&& isNumeric(avsenderId)
			&& avsenderId.length() == FOLKEREGISTER_IDENT_LENGTH) {
			return avsenderId.substring(DOUBLE_ZERO_PADDING.length());
		}
		return avsenderId;
	}

	private static String mapAvsenderMottakerIdType(String avsenderId) {
		if (!isNumeric(avsenderId)) {
			return null;
		}
		return switch (avsenderId.length()) {
			case FOLKEREGISTER_IDENT_LENGTH:
				if (isDoubleZeroPrefixed(avsenderId)) {
					yield AVSENDER_IDTYPE_ORGANISASJON;
				}
				yield AVSENDER_IDTYPE_PERSON;
			case NORSK_ORGANISASJONSNUMMER_LENGTH:
				yield AVSENDER_IDTYPE_ORGANISASJON;
			default:
				yield null;
		};
	}

	private static boolean isDoubleZeroPrefixed(String avsenderId) {
		return avsenderId.startsWith(DOUBLE_ZERO_PADDING);
	}


	private static boolean notNullOrEmpty(String string) {
		return string != null && !string.isBlank();
	}

	private static String appendFileType(String filename, String filetype) {
		return filename + "." + filetype;
	}
}
