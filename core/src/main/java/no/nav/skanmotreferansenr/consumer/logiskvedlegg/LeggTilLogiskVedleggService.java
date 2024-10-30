package no.nav.skanmotreferansenr.consumer.logiskvedlegg;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.logiskvedlegg.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
import no.nav.skanmotreferansenr.consumer.sts.data.STSResponse;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeggTilLogiskVedleggService {

    private final LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer;
    private final STSConsumer stsConsumer;

    @Autowired
    public LeggTilLogiskVedleggService(LeggTilLogiskVedleggConsumer leggTilLogiskVedleggConsumer, STSConsumer stsConsumer) {
        this.leggTilLogiskVedleggConsumer = leggTilLogiskVedleggConsumer;
        this.stsConsumer = stsConsumer;
    }

    public List<LeggTilLogiskVedleggResponse> leggTilLogiskVedlegg(OpprettJournalpostResponse opprettJournalpostResponse, FoerstesideMetadata foerstesideMetadata) {
        if (opprettJournalpostResponse == null || foerstesideMetadata == null) {
            return new ArrayList<>();
        }
        if(opprettJournalpostResponse.getDokumenter().isEmpty() || foerstesideMetadata.getVedleggsliste() == null) {
            return new ArrayList<>();
        }
        final String dokumentInfoId = opprettJournalpostResponse.getDokumenter().getFirst().dokumentInfoId();
        return foerstesideMetadata.getVedleggsliste().stream()
                .map(tittel -> leggTilLogiskVedlegg(dokumentInfoId, tittel))
                .filter(Objects::nonNull)
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
