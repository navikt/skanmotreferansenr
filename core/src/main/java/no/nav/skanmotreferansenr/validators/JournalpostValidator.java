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
