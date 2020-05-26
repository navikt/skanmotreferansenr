package no.nav.skanmotreferansenr.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Filepair {

    private final String name;
    private final byte[] pdf;
    private final byte[] xml;
}
