package no.nav.skanmotreferansenr.itest;

import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.consumer.foersteside.FoerstesidegeneratorConsumer;
import no.nav.skanmotreferansenr.consumer.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoerstesideIT extends AbstractItest {

	private FoerstesidegeneratorService foerstesidegeneratorService;

	@Autowired
	private SkanmotreferansenrProperties skanmotreferansenrProperties;

	@BeforeEach
	void setUpBeans() {
		super.setUpStubs();
		foerstesidegeneratorService = new FoerstesidegeneratorService(
				new FoerstesidegeneratorConsumer(new RestTemplateBuilder(), skanmotreferansenrProperties),
				new STSConsumer(new RestTemplateBuilder(), skanmotreferansenrProperties)
		);
	}

	@Test
	void shouldGetFoerstesideMetadata() {
		FoerstesideMetadata metadata = foerstesidegeneratorService.hentFoersteside(LOEPENR_OK).orElse(new FoerstesideMetadata());

		assertNull(metadata.getAvsender());
		assertEquals("12345678910", metadata.getBruker().getBrukerId());
		assertEquals("PERSON", metadata.getBruker().getBrukerType());
		assertEquals("AAP", metadata.getTema());
		assertNull(metadata.getBehandlingstema());
		assertEquals("Brev", metadata.getArkivtittel());
		assertEquals("VANL", metadata.getNavSkjemaId());
		assertEquals("9999", metadata.getEnhetsnummer());
		assertEquals(2, metadata.getVedleggsliste().size());
		assertTrue(metadata.getVedleggsliste().containsAll(List.of("Terminbekreftelse", "Dokumentasjon av inntekt")));
	}

	@Test
	void shouldGetEmptyOptionalIfNotExisting() {
		assertThat(foerstesidegeneratorService.hentFoersteside("222")).isEmpty();
	}
}