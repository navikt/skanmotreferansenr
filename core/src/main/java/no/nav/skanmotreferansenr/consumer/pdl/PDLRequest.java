package no.nav.skanmotreferansenr.consumer.pdl;

import java.util.Map;

public record PDLRequest(String query, Map<String, Object> variables) {
}
