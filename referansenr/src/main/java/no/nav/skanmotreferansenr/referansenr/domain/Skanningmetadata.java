package no.nav.skanmotreferansenr.referansenr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import no.nav.skanmotreferansenr.referansenr.validators.SkanningMetadataValidator;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    public void verifyFields() {
        SkanningMetadataValidator.validate(this);
    }

    @XmlElement(required = true, name = "journalpost")
    private Journalpost journalpost;

    @XmlElement(required = true, name = "skanningInfo")
    private SkanningInfo skanningInfo;
}
