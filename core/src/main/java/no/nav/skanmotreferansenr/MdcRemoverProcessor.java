package no.nav.skanmotreferansenr;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_BATCHNAVN;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_CALL_ID;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_FILENAME;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_ZIP_ID;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class MdcRemoverProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        MDC.remove(MDC_CALL_ID);
        MDC.remove(MDC_BATCHNAVN);
        MDC.remove(MDC_ZIP_ID);
        MDC.remove(MDC_FILENAME);
    }
}
