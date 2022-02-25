package no.nav.skanmotreferansenr.consumer.opprettjournalpost;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.consumer.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.consumer.sts.STSConsumer;
import no.nav.skanmotreferansenr.consumer.sts.data.STSResponse;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpprettJournalpostService {

    private final OpprettJournalpostConsumer opprettJournalpostConsumer;
    private final STSConsumer stsConsumer;
    private final OpprettJournalpostRequestMapper opprettJournalpostRequestMapper;

    @Autowired
    public OpprettJournalpostService(OpprettJournalpostConsumer opprettJournalpostConsumer, STSConsumer stsConsumer) {
        this.opprettJournalpostConsumer = opprettJournalpostConsumer;
        this.stsConsumer = stsConsumer;
        this.opprettJournalpostRequestMapper = new OpprettJournalpostRequestMapper();
    }

    public OpprettJournalpostResponse opprettJournalpost(Skanningmetadata skanningmetadata, FoerstesideMetadata foerstesideMetadata, Filepair filepair) {
        STSResponse stsResponse = stsConsumer.getSTSToken();
        final Journalpost journalpost = skanningmetadata.getJournalpost();
        String batchNavn = journalpost.getBatchnavn();
        try {
            log.info("Skanmotreferansenr oppretter journalpost fil={}, batch={}", filepair.getName(), batchNavn);
            OpprettJournalpostRequest request = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(skanningmetadata, foerstesideMetadata, filepair);
            OpprettJournalpostResponse response = opprettJournalpostConsumer.opprettJournalpost(stsResponse.getAccess_token(), request);
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
}
