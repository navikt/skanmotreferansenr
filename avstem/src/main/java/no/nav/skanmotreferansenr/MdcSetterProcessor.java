package no.nav.skanmotreferansenr;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotreferansenr.mdc.MDCConstants.EXCHANGE_AVSTEMMINGSFIL_NAVN;
import static no.nav.skanmotreferansenr.mdc.MDCConstants.MDC_CALL_ID;

public class MdcSetterProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		String avstemFilnavn = exchange.getProperty(EXCHANGE_AVSTEMMINGSFIL_NAVN, String.class);
		if (avstemFilnavn != null) {
			MDC.put(MDC_CALL_ID, avstemFilnavn);
		}
	}
}
