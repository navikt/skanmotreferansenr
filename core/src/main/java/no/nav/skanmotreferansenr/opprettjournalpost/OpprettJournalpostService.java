package no.nav.skanmotreferansenr.opprettjournalpost;

import no.nav.skanmotreferansenr.domain.Filepair;
import no.nav.skanmotreferansenr.domain.Skanningmetadata;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotreferansenr.opprettjournalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.sts.data.STSResponse;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.skanmotreferansenr.opprettjournalpost.OpprettJournalpostRequestMapper.generateRequestBody;

@Service
public class OpprettJournalpostService {

    private OpprettJournalpostConsumer opprettJournalpostConsumer;
    private STSConsumer stsConsumer;

    @Inject
    public OpprettJournalpostService(OpprettJournalpostConsumer opprettJournalpostConsumer, STSConsumer stsConsumer) {
        this.opprettJournalpostConsumer = opprettJournalpostConsumer;
        this.stsConsumer = stsConsumer;
    }

    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        STSResponse stsResponse = stsConsumer.getSTSToken();
        return opprettJournalpostConsumer.opprettJournalpost(stsResponse.getAccess_token(), request);
    }

    public OpprettJournalpostResponse opprettJournalpost(Skanningmetadata skanningmetadata, FoerstesideMetadata foerstesideMetadata, Filepair filePair) {
        OpprettJournalpostRequest request = generateRequestBody(skanningmetadata, foerstesideMetadata, filePair);
        return opprettJournalpost(request);
    }
}
