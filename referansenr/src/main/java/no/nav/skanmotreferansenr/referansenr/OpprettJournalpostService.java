package no.nav.skanmotreferansenr.referansenr;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.journalpostapi.JournalpostConsumer;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.Bruker;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.consumer.pdl.PdlGraphQLConsumer;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.mapper.BrukerMapper;
import no.nav.skanmotreferansenr.mapper.OpprettJournalpostRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpprettJournalpostService {

    private final JournalpostConsumer journalpostConsumer;
    private final PdlGraphQLConsumer pdlGraphQLConsumer;

    @Autowired
    public OpprettJournalpostService(JournalpostConsumer journalpostConsumer, PdlGraphQLConsumer pdlGraphQLConsumer) {
        this.journalpostConsumer = journalpostConsumer;
        this.pdlGraphQLConsumer = pdlGraphQLConsumer;
    }

    public OpprettJournalpostResponse opprettJournalpost(Skanningmetadata skanningmetadata, FoerstesideMetadata foerstesideMetadata, Filepair filepair) {
        final Journalpost journalpost = skanningmetadata.getJournalpost();
        String batchNavn = journalpost.getBatchnavn();
        try {
            log.info("Skanmotreferansenr oppretter journalpost fil={}, batch={}", filepair.getName(), batchNavn);

            var bruker = getBruker(foerstesideMetadata);
            OpprettJournalpostRequest request = OpprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(skanningmetadata, foerstesideMetadata, filepair, bruker);
            OpprettJournalpostResponse response = journalpostConsumer.opprettJournalpost(request);
            log.info("Skanmotreferansenr har opprettet journalpost, journalpostId={}, referansenr={}, fil={}, batch={}",
                    response.getJournalpostId(), journalpost.getReferansenummer(), filepair.getName(), batchNavn);
            return response;

        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.warn("Skanmotreferansenr feilet funksjonelt med oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            throw e;
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.warn("Skanmotreferansenr feilet teknisk med  oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            throw e;
        } catch (Exception e) {
            log.warn("Skanmotreferansenr feilet med ukjent feil ved oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            throw e;
        }
    }

    private Bruker getBruker(FoerstesideMetadata foerstesideMetadata) {
        var bruker = BrukerMapper.extractBruker(foerstesideMetadata);
        if (bruker != null && bruker.idType().equals(BrukerMapper.BRUKER_IDTYPE_PERSON)) {
            boolean identFinnes = pdlGraphQLConsumer.identFinnesIPdl(bruker.id());
            if (!identFinnes) return null;
        }
        return bruker;
    }
}
