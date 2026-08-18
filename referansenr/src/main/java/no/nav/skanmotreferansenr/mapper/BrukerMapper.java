package no.nav.skanmotreferansenr.mapper;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Bruker;

import java.util.regex.Pattern;

@Slf4j
public class BrukerMapper {
	// foerstesidegenerator domenet
	private static final String FOERSTESIDE_BRUKERTYPE_PERSON = "PERSON";
	private static final String FOERSTESIDE_BRUKERTYPE_ORGANISASJON = "ORGANISASJON";
	public static final String BRUKER_IDTYPE_PERSON = "FNR";
	static final String BRUKER_IDTYPE_ORGANISASJON = "ORGNR";
	private static final Pattern BRUKER_ID_PERSON_REGEX = Pattern.compile("[0-9]{11}");
	private static final Pattern BRUKER_ID_ORGANISASJON_REGEX = Pattern.compile("[0-9]{9}");

	public static Bruker extractBruker(FoerstesideMetadata foerstesideMetadata) {
		if (foerstesideMetadata.getBruker() == null || !isValidBruker(foerstesideMetadata)) {
			return null;
		}
		String id = foerstesideMetadata.getBruker().getBrukerId();
		String idType = foerstesideMetadata.getBruker().getBrukerType();
		if (FOERSTESIDE_BRUKERTYPE_PERSON.equals(idType)) {
			idType = BRUKER_IDTYPE_PERSON;
		} else if (FOERSTESIDE_BRUKERTYPE_ORGANISASJON.equals(idType)) {
			idType = BRUKER_IDTYPE_ORGANISASJON;
		}
		return new Bruker(id, idType);
	}

	private static boolean isValidBruker(FoerstesideMetadata metadata) {
		no.nav.skanmotreferansenr.consumer.foersteside.data.Bruker bruker = metadata.getBruker();
		if (FOERSTESIDE_BRUKERTYPE_PERSON.equals(bruker.getBrukerType())) {
			if (BRUKER_ID_PERSON_REGEX.matcher(bruker.getBrukerId()).matches()) {
				return true;
			}
			log.warn("Brukerid av type {} var ugyldig, setter bruker til null", FOERSTESIDE_BRUKERTYPE_PERSON);
		} else if (FOERSTESIDE_BRUKERTYPE_ORGANISASJON.equals(bruker.getBrukerType())) {
			if (BRUKER_ID_ORGANISASJON_REGEX.matcher(bruker.getBrukerId()).matches()) {
				return true;
			}
			log.warn("Brukerid av type {} var ugyldig, setter bruker til null", FOERSTESIDE_BRUKERTYPE_ORGANISASJON);
		} else {
			log.warn("Brukertype {} er ikke er en av følgende gyldige verdier: [{}, {}]. Setter bruker til null", bruker.getBrukerType(), FOERSTESIDE_BRUKERTYPE_PERSON, FOERSTESIDE_BRUKERTYPE_ORGANISASJON);
		}
		return false;
	}
}
