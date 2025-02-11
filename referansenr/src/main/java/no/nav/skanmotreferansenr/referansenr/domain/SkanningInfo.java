package no.nav.skanmotreferansenr.referansenr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlElement;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkanningInfo {

    @XmlElement(name = "fysiskPostboks")
    private String fysiskPostboks;

    @XmlElement(required = true, name = "strekkodePostboks")
    private String strekkodePostboks;
}
