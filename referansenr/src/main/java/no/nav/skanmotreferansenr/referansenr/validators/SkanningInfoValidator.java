package no.nav.skanmotreferansenr.referansenr.validators;

import java.util.Set;

public class SkanningInfoValidator {

    public static final Set<String> GYLDIGE_STREKKODE_POSTBOKS_VERDIER = Set.of("1400", "1402", "1405", "8888");

    public static boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return GYLDIGE_STREKKODE_POSTBOKS_VERDIER.contains(strekkodePostboks);
    }
}
