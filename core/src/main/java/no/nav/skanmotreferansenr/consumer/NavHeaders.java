package no.nav.skanmotreferansenr.consumer;


import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_CALL_ID;

public final class NavHeaders {
    public static final String NAV_CALL_ID = "Nav-Callid";

    private NavHeaders() {
        // Ingen instansiering
    }

    public static void setCustomNavHeaders(HttpHeaders headers) {
        if (MDC.get(MDC_CALL_ID) != null) {
            headers.add(NAV_CALL_ID, MDC.get(MDC_CALL_ID));
        }
    }
}
