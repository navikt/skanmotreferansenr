package no.nav.skanmotreferansenr.avstem;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;

public class AvstemAggregationStrategy extends AbstractListAggregationStrategy<String> {
	@Override
	public String getValue(Exchange exchange) {
		return exchange.getIn().getBody(String.class);
	}
}
