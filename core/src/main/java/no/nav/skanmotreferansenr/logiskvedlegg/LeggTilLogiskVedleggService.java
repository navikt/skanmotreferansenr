package no.nav.skanmotreferansenr.logiskvedlegg;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import no.nav.skanmotreferansenr.sts.data.STSResponse;
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
    private STSConsumer stsConsumer;

    @Inject
    public LeggTilLogiskVedleggService(LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer, STSConsumer stsConsumer) {
        this.leggTilLogiskVedleggConsumer = leggTilLogiskVedleggConsumer;
        this.stsConsumer = stsConsumer;
    }

    public List<LeggTilLogiskVedleggResponse> leggTilLogiskVedlegg(Optional<OpprettJournalpostResponse> opprettJournalpostResponse, Optional<FoerstesideMetadata> foerstesideMetadata) {
        if (opprettJournalpostResponse.isEmpty() || foerstesideMetadata.isEmpty() || opprettJournalpostResponse.get().getDokumenter().isEmpty()) {
            log.warn("Skanmotreferansenr kan ikke lagre vedlegg, da det mangler nødvendig data.");
            return new ArrayList<>();
        }
        String dokumentInfoId = opprettJournalpostResponse.get().getDokumenter().get(0).getDokumentInfoId();
        return foerstesideMetadata.get().getVedleggsliste().stream()
                .map(tittel -> leggTilLogiskVedlegg(dokumentInfoId, tittel))
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    private LeggTilLogiskVedleggResponse leggTilLogiskVedlegg(String dokumentInfoId, String tittel) {
        log.info("Skanmotreferansenr legger til logisk vedlegg, dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel);
        STSResponse stsResponse = stsConsumer.getSTSToken();
        LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder()
                .tittel(tittel)
                .build();
        try {
            return leggTilLogiskVedleggConsumer.leggTilLogiskVedlegg(request, dokumentInfoId, stsResponse.getAccess_token());
        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.warn("Skanmotreferansenr feilet funksjonelt med lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel, e);
            return null;
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.warn("Skanmotreferansenr feilet teknisk med lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel, e);
            return null;
        } catch (Exception e) {
            log.warn("Skanmotreferansenr feilet med ukjent feil ved lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel, e);
            return null;
        }
    }
}
