package no.nav.skanmotreferansenr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkanningInfo {

    @XmlElement(required = false, name = "fysiskPostboks")
    private String fysiskPostboks;

    @XmlElement(required = true, name = "strekkodePostboks")
    private String strekkodePostboks;
}
