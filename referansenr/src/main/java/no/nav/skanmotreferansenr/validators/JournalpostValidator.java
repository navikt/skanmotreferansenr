package no.nav.skanmotreferansenr.validators;

import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;

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
        return isNotEmpty(batchnavn);
    }

}
