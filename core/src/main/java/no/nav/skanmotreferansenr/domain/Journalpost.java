package no.nav.skanmotreferansenr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

import static no.nav.skanmotreferansenr.validators.JournalpostValidator.isValidReferansenr;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(required = true, name = "referansenummer")
    private String referansenummer;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlElement(required = true, name = "datoMottatt")
    private Date datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchnavn;

    @XmlElement(required = false, name = "filnavn")
    private String filnavn;

    @XmlElement(required = false, name = "endorsernr")
    private String endorsernr;

    public String getReferansenummerWithoutChecksum() {
        if(!isValidReferansenr(referansenummer)) {
            throw new InvalidMetadataException("Ugyldig referansenummer. Må være numerisk og 14 siffer langt. referansenummer=" + referansenummer);
        }
        return referansenummer.substring(0, 13);
    }
}
