package no.nav.skanmotreferansenr.validators;

public class SkanningInfoValidator {

    public static boolean isValidFysiskPostboks(String fysiskPostboks) {
        return isNonEmptyString(fysiskPostboks);
    }

    public static boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return "1400".equals(strekkodePostboks);
    }

    private static boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
