package no.nav.skanmotreferansenr;

import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ErrorMetricsProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        Object exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
        if(exception instanceof Throwable){
            DokCounter.incrementError((Throwable) exception);
        }
    }
}
