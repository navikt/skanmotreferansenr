package no.nav.skanmotreferansenr;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotreferansenr.exceptions.functional.SkanmotreferansenrUnzipperFunctionalException;
import no.nav.skanmotreferansenr.filomraade.FilomraadeService;
import no.nav.skanmotreferansenr.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.mdc.MDCGenerate;
import no.nav.skanmotreferansenr.metrics.DokCounter;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static no.nav.skanmotreferansenr.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotreferansenr.metrics.MetricLabels.PROCESS_NAME;
import static no.nav.skanmotreferansenr.unzipskanningmetadata.UnzipSkanningmetadataUtils.splitChecksumInReferansenummer;

@Slf4j
@Component
public class LesFraFilomraadeOgOpprettJournalpost {

    private final FilomraadeService filomraadeService;
    private final FoerstesidegeneratorService foerstesidegeneratorService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final DokCounter dokCounter;

    public LesFraFilomraadeOgOpprettJournalpost(FilomraadeService filomraadeService, FoerstesidegeneratorService foerstesidegeneratorService,
                                                OpprettJournalpostService opprettJournalpostService,
                                                DokCounter dokCounter) {
        this.filomraadeService = filomraadeService;
        this.foerstesidegeneratorService = foerstesidegeneratorService;
        this.opprettJournalpostService = opprettJournalpostService;
        this.dokCounter = dokCounter;
    }

    @Scheduled(cron = "${skanmotreferansenr.schedule}")
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
                setUpMDCforZip(zipName);

                log.info("Skanmotreferansenr laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList;
                try {
                    filepairList = Unzipper.unzipXmlPdf(filomraadeService.getZipFile(zipName));
                } catch (Exception e) {
                    log.error("Skanmotreferansenr klarte ikke lese zipfil {}", zipName, e);
                    dokCounter.incrementError(e);
                    moveZipFile(zipName); // TODO should this move to feilområde?
                    continue;
                }
                log.info("Skanmotreferansenr begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                    setUpMDCforFile(filepair.getName());

                    Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair);
                    if (skanningmetadata.isEmpty()) {
                        lastOppFilpar(filepair, zipName);
                    } else {
                        Optional<OpprettJournalpostResponse> response = opprettJournalpost(skanningmetadata, filepair);
                        if (response.isEmpty()) {
                            lastOppFilpar(filepair, zipName);
                        }
                    }
                    tearDownMDCforFile();
                });
                try {
                    moveZipFile(zipName);
                } catch (Exception e) {
                    dokCounter.incrementError(e);
                }
                tearDownMDCforZip();
            }
        } catch (Exception e) {
            log.error("Skanmotreferansenr ukjent feil oppstod i lesOgLagre, feilmelding={}", e.getMessage(), e);
            dokCounter.incrementError(e);
        } finally {
            // Feels like a leaky abstraction ...
            filomraadeService.disconnect();
        }
    }

    private Optional<Skanningmetadata> extractMetadata(Filepair filepair) {
        try {
            Skanningmetadata skanningmetadata = UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml());

            incrementMetadataMetrics(skanningmetadata);
            skanningmetadata.verifyFields();

            return Optional.of(splitChecksumInReferansenummer(skanningmetadata));
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotreferansenr klarte ikke unmarshalle.", filepair.getName(), e);
            dokCounter.incrementError(e);
            return Optional.empty();
        } catch (SkanmotreferansenrUnzipperFunctionalException e) {
            log.warn("Kunne ikke hente metadata fra {}, feilmelding={}", filepair.getName(), e.getMessage(), e);
            dokCounter.incrementError(e);
            return Optional.empty();
        }

    }

    private Optional<OpprettJournalpostResponse> opprettJournalpost(Optional<Skanningmetadata> skanningmetadata, Filepair filepair){
        try{
            Optional<FoerstesideMetadata> foerstesideMetadata = foerstesidegeneratorService.hentFoersteside(skanningmetadata.get().getJournalpost().getReferansenummer());
            OpprettJournalpostResponse response = opprettJournalpostService.opprettJournalpost(skanningmetadata, foerstesideMetadata, filepair);
            return Optional.of(response);
        } catch (Exception e) {
            dokCounter.incrementError(e);
            return Optional.empty();
        }
    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        try {
            log.info("Skanmotreferansenr laster opp fil til feilområde fil={} zipFil={}", filepair.getName(), zipName);
            String path = Utils.removeFileExtensionInFilename(zipName);
            filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
            filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
        } catch (Exception e) {
            log.error("Skanmotreferansenr feilet ved opplasting til feilområde fil={} zipFil={} feilmelding={}", filepair.getName(), zipName, e.getMessage(), e);
            dokCounter.incrementError(e);
        }
    }

    private void moveZipFile(String zipName) {
        try{
            filomraadeService.moveZipFile(zipName, "processed");
        } catch (Exception e) {
            dokCounter.incrementError(e);
        }
    }

    private void setUpMDCforZip(String zipname) {
        MDCGenerate.setZipId(zipname);
    }

    private void tearDownMDCforZip() {
        MDCGenerate.clearZipId();
    }

    private void setUpMDCforFile(String filename) {
        MDCGenerate.setFileName(filename);
        MDCGenerate.generateNewCallIdIfThereAreNone();
    }

    private void tearDownMDCforFile() {
        MDCGenerate.clearFilename();
        MDCGenerate.clearCallId();
    }

    private void incrementMetadataMetrics(Skanningmetadata skanningmetadata){
        final String STREKKODEPOSTBOKS = "strekkodePostboks";
        final String FYSISKPOSTBOKS = "fysiskPostboks";
        final String EMPTY = "empty";

        dokCounter.incrementCounter(Map.of(
                STREKKODEPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getStrekkodePostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY),
                FYSISKPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getFysiskPostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));
    }
}
