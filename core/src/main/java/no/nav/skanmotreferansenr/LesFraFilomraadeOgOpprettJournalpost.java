package no.nav.skanmotreferansenr;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrUnzipperFunctionalException;
import no.nav.skanmotreferansenr.filomraade.FilomraadeService;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.metrics.Metrics;
import no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostService;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotreferansenr.unzipskanningmetadata.Unzipper;
import no.nav.skanmotreferansenr.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;

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

    @Scheduled(initialDelay = 10000, fixedDelay = 30 * MINUTE)
    public void scheduledJob() {
        lesOgLagreZipfiler();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "lesOgLagreZipfiler"}, percentiles = {0.5, 0.95}, histogram = true)
    public void lesOgLagreZipfiler() {
        List<String> processedZipFiles = new ArrayList<>();
        try {
            List<String> filenames = filomraadeService.getFileNames();
            log.info("Skanmotreferansenr fant {} zipfiler på sftp server: {}", filenames.size(), filenames);
            for (String zipName : filenames) {

                log.info("Skanmotreferansenr laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList;
                try {
                    filepairList = Unzipper.unzipXmlPdf(filomraadeService.getZipFile(zipName));
                } catch (Exception e) {
                    log.error("Skanmotreferansenr klarte ikke lese zipfil {}", zipName, e);
                    processedZipFiles.add(zipName);
                    continue;
                }

                log.info("Skanmotreferansenr begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                    Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair);
                    if (skanningmetadata.isEmpty()) {
                        lastOppFilpar(filepair, zipName);
                    } else {
                        Optional<FoerstesideMetadata> foerstesideMetadata = foerstesidegeneratorService.hentFoersteside(skanningmetadata.get().getJournalpost().getReferansenummer());
                        Optional<OpprettJournalpostResponse> response = opprettJournalpostService.opprettJournalpost(skanningmetadata, foerstesideMetadata, filepair);
                        try {
                            if (response.isEmpty()) {
                                lastOppFilpar(filepair, zipName);
                            }
                        } catch (Exception e) {
                            log.error("Skanmotreferansenr feilet ved opplasting til feilområde fil={} zipFil={} feilmelding={}", filepair.getName(), zipName, e.getMessage(), e);
                        }
                    }
                });
                processedZipFiles.add(zipName);
            }
        } catch (Exception e) {
            log.error("Skanmotreferansenr ukjent feil oppstod i lesOgLagre, feilmelding={}", e.getMessage(), e);
        } finally {
            filomraadeService.moveZipFiles(processedZipFiles, "processed");

            // Feels like a leaky abstraction ...
            filomraadeService.disconnect();
        }
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
        log.info("Skanmotreferansenr laster opp fil til feilområde fil={} zipFil={}", filepair.getName(), zipName);
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
    }
}
