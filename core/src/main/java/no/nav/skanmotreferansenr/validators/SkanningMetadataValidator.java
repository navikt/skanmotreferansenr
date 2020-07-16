package no.nav.skanmotreferansenr.validators;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;

@Slf4j
public class SkanningMetadataValidator {

    public static void validate(Skanningmetadata skanningmetadata) {
        verfiyMetadataIsValid(skanningmetadata);
    }

    private static void verfiyMetadataIsValid(Skanningmetadata skanningmetadata) {
        if (null == skanningmetadata) {
            throw new InvalidMetadataException("Skanningmetadata is null");
        }
        verifyJournalpostIsValid(skanningmetadata.getJournalpost());
        verifySkanningInfoIsValid(skanningmetadata.getSkanningInfo());
    }

    private static void verifyJournalpostIsValid(Journalpost journalpost) {
        if (null == journalpost) {
            throw new InvalidMetadataException("Journalpost is null");
        }
        if (!JournalpostValidator.isValidReferansenr(journalpost.getReferansenummer())){
            throw new InvalidMetadataException("Referansenr is not valid: " + journalpost.getReferansenummer());
        }
        if (!JournalpostValidator.isValidMottakskanal(journalpost.getMottakskanal())) {
            throw new InvalidMetadataException("Mottakskanal is not valid: " + journalpost.getMottakskanal());
        }
        if (!JournalpostValidator.isValidDatoMottatt(journalpost.getDatoMottatt())) {
            throw new InvalidMetadataException("DatoMottatt is not valid: " + journalpost.getDatoMottatt());
        }
        if (!JournalpostValidator.isValidBatchNavn(journalpost.getBatchnavn())) {
            throw new InvalidMetadataException("Batchnavn is not valid: " + journalpost.getBatchnavn());
        }
    }

    private static void verifySkanningInfoIsValid(SkanningInfo skanningInfo) {
        if (skanningInfo == null) {
            throw new InvalidMetadataException("SkanningInfo is null");
        }
        if (!SkanningInfoValidator.isValidStrekkodePostboks(skanningInfo.getStrekkodePostboks())) {
            throw new InvalidMetadataException("Strekkodepostboks is not valid: " + skanningInfo.getStrekkodePostboks() +
                    ". Must be one of: " + SkanningInfoValidator.strekkodePostboksVerdier);
        }
    }
}
