package no.nav.skanmotreferansenr.referansenr;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.FoerstesidegeneratorService;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.journalpostapi.LeggTilLogiskVedleggService;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.referansenr.domain.Filepair;
import no.nav.skanmotreferansenr.referansenr.domain.SkanningInfo;
import no.nav.skanmotreferansenr.referansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.metrics.DokCounter;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.logstash.logback.util.StringUtils.isEmpty;

@Slf4j
@Component
public class PostboksReferansenrService {

    private final FoerstesidegeneratorService foerstesidegeneratorService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final LeggTilLogiskVedleggService leggTilLogiskVedleggService;
    private final String EMPTY = "empty";

    @Autowired
    public PostboksReferansenrService(FoerstesidegeneratorService foerstesidegeneratorService,
                                      OpprettJournalpostService opprettJournalpostService,
                                      LeggTilLogiskVedleggService leggTilLogiskVedleggService) {
        this.foerstesidegeneratorService = foerstesidegeneratorService;
        this.opprettJournalpostService = opprettJournalpostService;
        this.leggTilLogiskVedleggService = leggTilLogiskVedleggService;
    }

    @Handler
    public void behandleForsendelse(@Body PostboksReferansenrEnvelope envelope) {
        final Skanningmetadata skanningmetadata = envelope.getSkanningmetadata();
        skanningmetadata.verifyFields();
        incrementMetadataMetrics(skanningmetadata);
        final String referansenummerWithoutChecksum = skanningmetadata.getJournalpost().getReferansenummerWithoutChecksum();
        FoerstesideMetadata foerstesideMetadata = foerstesidegeneratorService.hentFoersteside(referansenummerWithoutChecksum).orElse(new FoerstesideMetadata());
        OpprettJournalpostResponse opprettjournalpostResponse = opprettJournalpostService.opprettJournalpost(skanningmetadata, foerstesideMetadata, Filepair.builder()
                .name(envelope.getFilebasename())
                .xml(envelope.getXml())
                .pdf(envelope.getPdf())
                .build());
        List<LeggTilLogiskVedleggResponse> leggTilLogiskVedleggResponses = leggTilLogiskVedleggService.leggTilLogiskVedlegg(opprettjournalpostResponse, foerstesideMetadata);
        if (!leggTilLogiskVedleggResponses.isEmpty()) {
            logLogiskVedleggResponses(leggTilLogiskVedleggResponses);
        }
        incrementTemaCounter(foerstesideMetadata.getTema());

    }

    private void logLogiskVedleggResponses(List<LeggTilLogiskVedleggResponse> leggTilLogiskVedleggResponses) {
        List<String> logiskVedleggIds = leggTilLogiskVedleggResponses.stream()
                .filter(Objects::nonNull)
                .map(LeggTilLogiskVedleggResponse::getLogiskVedleggId)
                .collect(Collectors.toList());
        log.info("Skanmotreferansenr lagret logisk vedlegg med logiskVedleggIds: {}", logiskVedleggIds);
    }

    private void incrementTemaCounter(String tema) {
        DokCounter.incrementCounter(DokCounter.TEMA, List.of(DokCounter.DOMAIN, DokCounter.REFERANSENR, DokCounter.TEMA, isEmpty(tema) ? EMPTY : tema));
    }

    private void incrementMetadataMetrics(Skanningmetadata skanningmetadata) {
        final String STREKKODEPOSTBOKS = "strekkodePostboks";
        final String FYSISKPOSTBOKS = "fysiskPostboks";

        DokCounter.incrementCounter(Map.of(
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
