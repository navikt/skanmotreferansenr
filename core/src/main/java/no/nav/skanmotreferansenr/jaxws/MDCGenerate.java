package no.nav.skanmotreferansenr.jaxws;

import org.slf4j.MDC;

import java.util.UUID;

import static no.nav.skanmotreferansenr.jaxws.MDCConstants.MDC_CALL_ID;

public class MDCGenerate {

    public static void generateNewCallIdIfThereAreNone() {
        if (MDC.get(MDC_CALL_ID) == null) {
            MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
        }
    }
}
