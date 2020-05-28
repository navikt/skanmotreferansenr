package no.nav.skanmotreferansenr.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilepairWithMetadata {

    private final Skanningmetadata skanningmetadata;
    private final byte[] pdf;
    private final byte[] xml;
}
