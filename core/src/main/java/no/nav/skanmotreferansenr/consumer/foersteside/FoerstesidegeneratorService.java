package no.nav.skanmotreferansenr.consumer.foersteside;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.functional.HentMetadataFoerstesideFinnesIkkeFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class FoerstesidegeneratorService {

    private final FoerstesidegeneratorConsumer foerstesidegeneratorConsumer;

    @Autowired
    public FoerstesidegeneratorService(FoerstesidegeneratorConsumer foerstesidegeneratorConsumer) {
        this.foerstesidegeneratorConsumer = foerstesidegeneratorConsumer;
    }

    public Optional<FoerstesideMetadata> hentFoersteside(String loepenr) {
        try {
            final FoerstesideMetadata foerstesideMetadata = foerstesidegeneratorConsumer.hentFoersteside(loepenr);
            log.info("Skanmotreferansenr har hentet førsteside. løpenummer={}.", loepenr);
            return Optional.of(foerstesideMetadata);
        } catch (HentMetadataFoerstesideFinnesIkkeFunctionalException e) {
            log.warn("Fant ikke metadata for førsteside. løpenummer={}.", loepenr, e);
            return Optional.empty();
        } catch (AbstractSkanmotreferansenrFunctionalException e) {
            log.error("Skanmotreferansenr feilet funksjonelt med henting av førsteside. løpenummer={}.", loepenr, e);
            throw e;
        } catch (AbstractSkanmotreferansenrTechnicalException e) {
            log.error("Skanmotreferansenr feilet teknisk med henting av førsteside. løpenummer={}.", loepenr, e);
            throw e;
        } catch (Exception e) {
            log.error("Skanmotreferansenr feilet med ukjent feil ved henting av førsteside. løpenummer={}.", loepenr, e);
            throw e;
        }
    }
}
