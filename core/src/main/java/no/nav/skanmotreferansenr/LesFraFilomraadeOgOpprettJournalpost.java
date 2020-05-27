package no.nav.skanmotreferansenr;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrUnzipperFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.filomraade.FilomraadeService;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostService;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotreferansenr.unzipskanningmetadata.Unzipper;
import no.nav.skanmotreferansenr.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class LesFraFilomraadeOgOpprettJournalpost {

    private final FilomraadeService filomraadeService;
    private final FoerstesidegeneratorService foerstesidegeneratorService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final int MINUTE = 60_000;

    public LesFraFilomraadeOgOpprettJournalpost(FilomraadeService filomraadeService, FoerstesidegeneratorService foerstesidegeneratorService,
                                                OpprettJournalpostService opprettJournalpostService) {
        this.filomraadeService = filomraadeService;
        this.foerstesidegeneratorService = foerstesidegeneratorService;
        this.opprettJournalpostService = opprettJournalpostService;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 10 * MINUTE)
    public void scheduledJob() {
        lesOgLagre();
    }

    public void lesOgLagre() {
        try {
            List<String> filenames = filomraadeService.getFileNames();
            log.info("Skanmotreferansenr fant {} zipfiler på sftp server", filenames.size());
            for (String zipName : filenames) {
                AtomicBoolean safeToDeleteZipFile = new AtomicBoolean(true);

                log.info("Skanmotreferansenr laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList = Unzipper.unzipXmlPdf(filomraadeService.getZipFile(zipName)); // TODO feilhåndtering hvis zipfil ikke er lesbar.
                log.info("Skanmotreferansenr begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                    Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair);
                    Optional<FoerstesideMetadata> foerstesideMetadata = getFoerstesideMetadata(skanningmetadata);
                    Optional<OpprettJournalpostResponse> response = opprettJournalpost(filepair, skanningmetadata, foerstesideMetadata);
                    try {
                        if (response.isEmpty()) {
                            log.warn("Skanmotreferansenr laster opp fil til feilområde fil={} zipFil={}", filepair.getName(), zipName);
                            lastOppFilpar(filepair, zipName);
                            log.warn("Skanmotreferansenr laster opp fil til feilområde fil={} zipFil={}", filepair.getName(), zipName);
                        }
                    } catch (Exception e) {
                        log.error("Skanmotreferansenr feilet ved opplasting til feilområde fil={} zipFil={} feilmelding={}", filepair.getName(), zipName, e.getMessage(), e);
                        safeToDeleteZipFile.set(false);
                    }
                });

                if (safeToDeleteZipFile.get()) {
                    filomraadeService.moveZipFile(zipName, "processed");
                }
            }
        } catch (Exception e) {
            log.error("Skanmotreferansenr ukjent feil oppstod i lesOgLagre, feilmelding={}", e.getMessage(), e);
        } finally {
            // Feels like a leaky abstraction ...
            filomraadeService.disconnect();
        }
    }

    private Optional<FoerstesideMetadata> getFoerstesideMetadata(Optional<Skanningmetadata> skanningmetadata) {
        FoerstesideMetadata response = null;

        if (skanningmetadata.isEmpty()) {
            return Optional.empty();
        }

        String referansenr = skanningmetadata.get().getJournalpost().getReferansenummer();
        try {
            response = foerstesidegeneratorService.hentFoersteside(referansenr);
        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.error("Skanmotreferansenr feilet funskjonelt med henting av foerstesidemetadata referansenr={}", referansenr, e);
            return Optional.empty();
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.error("Skanmotreferansenr feilet teknisk med henting av foerstesidemetadata referansenr={}", referansenr, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Skanmotreferansenr feilet med ukjent feil ved henting av foerstesidemetadata referansenr={}", referansenr, e);
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private Optional<OpprettJournalpostResponse> opprettJournalpost(Filepair filepair, Optional<Skanningmetadata> skanningmetadata, Optional<FoerstesideMetadata> foerstesideMetadata) {

        OpprettJournalpostResponse response = null;

        if (skanningmetadata.isEmpty() || foerstesideMetadata.isEmpty()) {
            return Optional.empty();
        }
        String batchNavn = skanningmetadata.map(Skanningmetadata::getJournalpost).map(Journalpost::getBatchNavn).orElse(null);
        try {
            log.info("Skanmotreferansenr oppretter journalpost fil={}, batch={}", filepair.getName(), batchNavn);
            response = opprettJournalpostService.opprettJournalpost(skanningmetadata.get(), foerstesideMetadata.get(), filepair);
            log.info("Skanmotreferansenr har opprettet journalpost, journalpostId={}, fil={}, batch={}", response.getJournalpostId(), filepair.getName(), batchNavn);
        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.error("Skanmotreferansenr feilet funskjonelt med oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            return Optional.empty();
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.error("Skanmotreferansenr feilet teknisk med  oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Skanmotreferansenr feilet med ukjent feil ved oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private Optional<Skanningmetadata> extractMetadata(Filepair filepair) {
        try {
            return Optional.of(UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml()));
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotreferansenr klarte ikke unmarshalle.", filepair.getName(), e);
            return Optional.empty();
        } catch (SkanmotreferansenrUnzipperFunctionalException e) {
            log.warn("Kunne ikke hente metadata fra {}, feilmelding={}", filepair.getName(), e.getMessage(), e);
            return Optional.empty();
        }

    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
    }
}
