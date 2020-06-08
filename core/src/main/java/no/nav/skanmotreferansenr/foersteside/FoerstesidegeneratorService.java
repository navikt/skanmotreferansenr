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

    private final String BRUKERTYPE_PERSON = "PERSON";
    private final String BRUKERTYPE_ORG = "ORGANISASJON";

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
            FoerstesideMetadata metadata = foerstesidegeneratorConsumer.hentFoersteside(stsResponse.getAccess_token(), loepenr);
            if (!isValidBruker(metadata)) {
                metadata.setBruker(null);
            }
            return Optional.of(metadata);
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

    private boolean isValidBruker(FoerstesideMetadata metadata) {
        if (BRUKERTYPE_PERSON.equals(metadata.getBruker().getBrukerType())) {
            return metadata.getBruker().getBrukerId().length() == 11;
        } if (BRUKERTYPE_ORG.equals(metadata.getBruker().getBrukerType())) {
            return metadata.getBruker().getBrukerId().length() == 9;
        }
        return false;
    }
}
