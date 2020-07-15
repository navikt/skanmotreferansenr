package no.nav.skanmotreferansenr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.skanmotreferansenr.validators.SkanningMetadataValidator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
