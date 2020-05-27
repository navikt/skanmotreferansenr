package no.nav.skanmotreferansenr.foersteside;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import no.nav.skanmotreferansenr.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.sts.STSConsumer;
import no.nav.skanmotreferansenr.sts.data.STSResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

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

    public Optional<FoerstesideMetadata> hentFoersteside(String loepenr) {
        STSResponse stsResponse = stsConsumer.getSTSToken();
        try {
            return Optional.of(foerstesidegeneratorConsumer.hentFoersteside(stsResponse.getAccess_token(), loepenr));
        } catch (HentMetadataFoerstesideFinnesIkkeFunctionalException e) {
            log.warn("Fant ikke metadata for foersteside med lopenummer {}", loepenr, e);
            return Optional.empty();
        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.error("Skanmotreferansenr feilet funksjonelt med henting av foerstesidemetadata loepenr={}", loepenr, e);
            return Optional.empty();
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.error("Skanmotreferansenr feilet teknisk med henting av foerstesidemetadata loepenr={}", loepenr, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Skanmotreferansenr feilet med ukjent feil ved henting av foerstesidemetadata loepenr={}", loepenr, e);
            return Optional.empty();
        }
    }
}
