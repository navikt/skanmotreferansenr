package no.nav.skanmotreferansenr.mdc;

import org.slf4j.MDC;

import java.util.UUID;

import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_FILENAME;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_ZIP_ID;

public class MDCGenerate {

    public static void generateNewCallIdIfThereAreNone() {
        if (MDC.get(MDC_NAV_CALL_ID) == null) {
            MDC.put(MDC_NAV_CALL_ID, UUID.randomUUID().toString());
        }
    }

    public static void clearCallId() {
        if (MDC.get(MDC_NAV_CALL_ID) != null) {
            MDC.remove(MDC_NAV_CALL_ID);
        }
    }

    public static void setZipId(String batchId) {
        MDC.put(MDC_ZIP_ID, batchId);
    }

    public static void clearZipId() {
        if (MDC.get(MDC_ZIP_ID) != null) {
            MDC.remove(MDC_ZIP_ID);
        }
    }

    public static void setFileName(String filename) {
        MDC.put(MDC_FILENAME, filename);
    }

    public static void clearFilename() {
        if (MDC.get(MDC_FILENAME) != null) {
            MDC.remove(MDC_FILENAME);
        }
    }

    public static void clearMdc() {
        MDC.clear();
    }
}
