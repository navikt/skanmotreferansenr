package no.nav.skanmotreferansenr.validators;

import java.util.Set;

public class SkanningInfoValidator {

    public static Set<String> strekkodePostboksVerdier = Set.of("1400", "1402", "1405", "8888");

    public static boolean isValidFysiskPostboks(String fysiskPostboks) {
        return isNonEmptyString(fysiskPostboks);
    }

    public static boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return strekkodePostboksVerdier.contains(strekkodePostboks);
    }

    private static boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
