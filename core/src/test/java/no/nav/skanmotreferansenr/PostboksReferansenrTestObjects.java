package no.nav.skanmotreferansenr;

import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class PostboksReferansenrTestObjects {
	public static final String FILEBASENAME = "pb1400";
	public static final String ZIPNAME = "pb1400.zip";
	public static final String FYSISK_POSTBOKS = "f-1400";
	public static final String STREKKODE_POSTBOKS = "1400";
	public static final byte[] XML_FIL = "pb1400.xml".getBytes();
	public static final byte[] PDF_FIL = "pb1400.pdf".getBytes();
	public static final String BATCH_NAVN = "pb1400-2020";
	public static final String MOTTAKSKANAL = "SKAN_IM";
	public static final String ENDORSERNR = "12345";
	public static final Date DATO_MOTTATT = Date.from(LocalDate.parse("2020-04-06").atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	public static final String REFERANSENR = "1111111111111";
	public static final String REFERANSENR_CHECKSUM = "8";
	public static final String REFERANSENR_WITH_CHECKSUM = REFERANSENR + REFERANSENR_CHECKSUM;

	static PostboksReferansenrEnvelope createEnvelope() {
		return createBaseEnvelope()
				.build();
	}

	static PostboksReferansenrEnvelope.PostboksReferansenrEnvelopeBuilder createBaseEnvelope() {
		return PostboksReferansenrEnvelope.builder()
				.filebasename(FILEBASENAME)
				.zipname(ZIPNAME)
				.skanningmetadata(Skanningmetadata.builder()
						.journalpost(Journalpost.builder()
								.mottakskanal(MOTTAKSKANAL)
								.datoMottatt(DATO_MOTTATT)
								.batchnavn(BATCH_NAVN)
								.endorsernr(ENDORSERNR)
								.referansenummer(REFERANSENR_WITH_CHECKSUM)
								.build())
						.skanningInfo(SkanningInfo.builder()
								.fysiskPostboks(FYSISK_POSTBOKS)
								.strekkodePostboks(STREKKODE_POSTBOKS)
								.build())
						.build())
				.xml(XML_FIL)
				.pdf(PDF_FIL);
	}
}
