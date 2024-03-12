package no.nav.skanmotreferansenr.mdc;

import java.util.Set;

public class MDCConstants {
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_ZIP_ID = "zipId";
    public static final String MDC_FILENAME = "filename";
    public static final String MDC_CALL_ID = "callId";
    public static final String MDC_BATCHNAVN = "batchnavn";

    public static Set<String> ALL_KEYS = Set.of(MDC_CALL_ID);

    private MDCConstants() {
        //no-op
    }
}
