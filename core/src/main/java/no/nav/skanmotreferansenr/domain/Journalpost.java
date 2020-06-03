package no.nav.skanmotreferansenr.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(required = true, name = "referansenummer")
    private String referansenummer;
    private String referansenrChecksum;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlElement(required = true, name = "datomottatt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchNavn;

    @XmlElement(required = false, name = "filnavn")
    private String filNavn;

    @XmlElement(required = false, name = "endorsernr")
    private String endorsernr;

}
