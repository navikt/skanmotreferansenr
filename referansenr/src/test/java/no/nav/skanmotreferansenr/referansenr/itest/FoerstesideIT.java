package no.nav.skanmotreferansenr.referansenr.itest;

import no.nav.skanmotreferansenr.consumer.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoerstesideIT extends AbstractItest {

	@Autowired
	private FoerstesidegeneratorService foerstesidegeneratorService;

	@BeforeEach
	void setUpBeans() {
		super.stubAzureToken();
		setUpStubs();
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

	@Test
	void shouldGetFoerstesideMetadataWithAvsender() {
		stubFoerstesideMedAvsender("12345678", "HPRNR");

		Optional<FoerstesideMetadata> metadata = foerstesidegeneratorService.hentFoersteside(LOEPENR_OK);

		assertThat(metadata)
				.get()
				.extracting(FoerstesideMetadata::getAvsender)
				.satisfies(avsender -> {
					assertThat(avsender.getAvsenderId()).isEqualTo("12345678");
					assertThat(avsender.getAvsenderIdType()).isEqualTo("HPRNR");
					assertThat(avsender.getAvsenderNavn()).isEqualTo("Avsender Navn");
				});
	}
}