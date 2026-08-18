package no.nav.skanmotreferansenr.consumer.pdl;

import java.util.List;

public record PDLHentIdenter(
	PDLIdenter hentIdenter
) {
	public record PDLIdenter(
		List<Ident> identer
	) {
	}
}
