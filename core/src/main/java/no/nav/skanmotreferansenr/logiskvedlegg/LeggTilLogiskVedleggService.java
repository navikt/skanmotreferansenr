package no.nav.skanmotreferansenr.logiskvedlegg;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.opprettjournalpost.data.DokumentInfo;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeggTilLogiskVedleggService {

    private LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer;

    @Inject
    public LeggTilLogiskVedleggService(LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer) {
        this.leggTilLogiskVedleggConsumer = leggTilLogiskVedleggConsumer;
    }

    public List<LeggTilLogiskVedleggResponse> leggTilLogiskVedlegg(Optional<OpprettJournalpostResponse> opprettJournalpostResponse) {
        if (opprettJournalpostResponse.isEmpty() || opprettJournalpostResponse.get().getDokumenter() == null) {
            return new ArrayList<>();
        }

        return opprettJournalpostResponse.get().getDokumenter().stream()
                .map(this::leggTilLogiskVedlegg)
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    private LeggTilLogiskVedleggResponse leggTilLogiskVedlegg(DokumentInfo dokumentInfo) {
        log.info("Skanmotreferansenr legger til logisk vedlegg, dokumentInfoId={}, dokumentTittel={}", dokumentInfo.getDokumentInfoId(), dokumentInfo.getTittel());
        LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder()
                .tittel(dokumentInfo.getTittel())
                .build();
        try {
            return leggTilLogiskVedleggConsumer.leggTilLogiskVedlegg(request, dokumentInfo.getDokumentInfoId());
        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.warn("Skanmotreferansenr feilet funksjonelt med lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfo.getDokumentInfoId(), dokumentInfo.getTittel(), e);
            return null;
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.warn("Skanmotreferansenr feilet teknisk med lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfo.getDokumentInfoId(), dokumentInfo.getTittel(), e);
            return null;
        } catch (Exception e) {
            log.warn("Skanmotreferansenr feilet med ukjent feil ved lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfo.getDokumentInfoId(), dokumentInfo.getTittel(), e);
            return null;
        }
    }
}
