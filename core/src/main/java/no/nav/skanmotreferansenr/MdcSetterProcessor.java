package no.nav.skanmotreferansenr;

import no.nav.skanmotreferansenr.mdc.MDCConstants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotreferansenr.PostboksReferansenrRoute.PROPERTY_FORSENDELSE_BATCHNAVN;
import static no.nav.skanmotreferansenr.PostboksReferansenrRoute.PROPERTY_FORSENDELSE_FILEBASENAME;
import static no.nav.skanmotreferansenr.PostboksReferansenrRoute.PROPERTY_FORSENDELSE_ZIPNAME;


/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class MdcSetterProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        final String exchangeId = exchange.getExchangeId();
        if (exchangeId != null) {
            MDC.put(MDCConstants.MDC_CALL_ID, exchangeId);
        }
        final String batchNavn = exchange.getProperty(PROPERTY_FORSENDELSE_BATCHNAVN, String.class);
        if (batchNavn != null) {
            MDC.put(MDCConstants.MDC_BATCHNAVN, batchNavn);
        }

        final String zipId = exchange.getProperty(PROPERTY_FORSENDELSE_ZIPNAME, String.class);
        if (zipId != null) {
            MDC.put(MDCConstants.MDC_ZIP_ID, zipId);
        }
        final String filename = exchange.getProperty(PROPERTY_FORSENDELSE_FILEBASENAME, String.class);
        if (filename != null) {
            MDC.put(MDCConstants.MDC_FILENAME, filename);
        }
    }
}
