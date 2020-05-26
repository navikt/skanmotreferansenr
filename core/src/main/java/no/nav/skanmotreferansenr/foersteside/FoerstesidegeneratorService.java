package no.nav.skanmotreferansenr.foersteside;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.foerstesidegenerator.api.v1.GetFoerstesideResponse;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.HentMetadataFoerstesideTechnicalException;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import no.nav.skanmotreferansenr.sts.data.STSResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Slf4j
public class FoerstesidegeneratorService {

    private FoerstesidegeneratorConsumer foerstesidegeneratorConsumer;
    private STSConsumer stsConsumer;

    @Inject
    public FoerstesidegeneratorService(FoerstesidegeneratorConsumer foerstesidegeneratorConsumer, STSConsumer stsConsumer) {
        this.foerstesidegeneratorConsumer = foerstesidegeneratorConsumer;
        this.stsConsumer = stsConsumer;
    }

    public GetFoerstesideResponse hentFoersteside(String loepenr) throws HentMetadataFoerstesideTillaterIkkeTilknyttingFunctionalException,
            HentMetadataFoerstesideFunctionalException, HentMetadataFoerstesideTechnicalException {
        STSResponse stsResponse = stsConsumer.getSTSToken();
        try {
            return foerstesidegeneratorConsumer.hentFoersteside(stsResponse.getAccess_token(), loepenr);
        } catch (HentMetadataFoerstesideFinnesIkkeFunctionalException e) {
            log.warn("Fant ikke metadata for foersteside med lopenummer {}", loepenr, e);
            return null;
        }
    }
}
