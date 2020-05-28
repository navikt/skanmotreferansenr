package no.nav.skanmotreferansenr.validators;

import java.util.Date;

public class JournalpostValidator {

    public static boolean isValidReferansenr(String referansenr) {
        return isNumeric(referansenr) && referansenr.length() == 14;
    }

    public static boolean isValidMottakskanal(String mottakskanal) {
        return "SKAN_IM".equals(mottakskanal);
    }

    public static boolean isValidDatoMottatt(Date datoMottatt) {
        return datoMottatt != null;
    }

    public static boolean isValidBatchNavn(String batchnavn) {
        return isNonEmptyString(batchnavn);
    }

    public static boolean isValidFilnavn(String filnavn) {
        if (isNonEmptyString(filnavn) && filnavn.length() >= 5) {
            String fileEnding = filnavn.substring(filnavn.length() - 4);
            return '.' == fileEnding.charAt(0);
        }
        return false;
    }

    public static boolean isValidEndorsernr(String endorsernr) {
        return isNonEmptyString(endorsernr);
    }

    private static boolean isNumeric(String string) {
        if (isNonEmptyString(string)) {
            try {
                Long.parseLong(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isNonEmptyString(String string) {
        if (string != null) {
            return string.length() > 0;
        }
        return false;
    }

}
