package no.nav.skanmotreferansenr.opprettjournalpost;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Journalpost;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import no.nav.skanmotreferansenr.sts.data.STSResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;

@Slf4j
@Service
public class OpprettJournalpostService {

    private OpprettJournalpostConsumer opprettJournalpostConsumer;
    private STSConsumer stsConsumer;
    private OpprettJournalpostRequestMapper opprettJournalpostRequestMapper;

    @Inject
    public OpprettJournalpostService(OpprettJournalpostConsumer opprettJournalpostConsumer, STSConsumer stsConsumer) {
        this.opprettJournalpostConsumer = opprettJournalpostConsumer;
        this.stsConsumer = stsConsumer;
        this.opprettJournalpostRequestMapper = new OpprettJournalpostRequestMapper();
    }

    public Optional<OpprettJournalpostResponse> opprettJournalpost(Optional<Skanningmetadata> skanningmetadata, Optional<FoerstesideMetadata> foerstesideMetadata, Filepair filepair) {

        if (skanningmetadata.isEmpty() || foerstesideMetadata.isEmpty()) {
            return Optional.empty();
        }
        STSResponse stsResponse = stsConsumer.getSTSToken();
        String batchNavn = skanningmetadata.map(Skanningmetadata::getJournalpost).map(Journalpost::getBatchNavn).orElse(null);
        try {

            log.info("Skanmotreferansenr oppretter journalpost fil={}, batch={}", filepair.getName(), batchNavn);
            OpprettJournalpostRequest request = opprettJournalpostRequestMapper.mapMetadataToOpprettJournalpostRequest(skanningmetadata.get(), foerstesideMetadata.get(), filepair);
            OpprettJournalpostResponse response = opprettJournalpostConsumer.opprettJournalpost(stsResponse.getAccess_token(), request);
            log.info("Skanmotreferansenr har opprettet journalpost, journalpostId={}, fil={}, batch={}", response.getJournalpostId(), filepair.getName(), batchNavn);
            return Optional.of(response);

        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.error("Skanmotreferansenr feilet funksjonelt med oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            return Optional.empty();
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.error("Skanmotreferansenr feilet teknisk med  oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Skanmotreferansenr feilet med ukjent feil ved oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            return Optional.empty();
        }
    }
}
