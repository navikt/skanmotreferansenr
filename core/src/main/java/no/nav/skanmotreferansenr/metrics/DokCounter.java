package no.nav.skanmotreferansenr.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Component
public class DokCounter {
    private static final String DOK_SKANMOTREFERANSENR = "dok_skanmotreferansenr_";
    private static final String TOTAL = "_total";
    private static final String EXCEPTION = "exception";
    private static final String ERROR_TYPE = "error_type";
    private static final String EXCEPTION_NAME = "exception_name";
    private static final String FUNCTIONAL_ERROR = "functional";
    private static final String TECHNICAL_ERROR = "technical";
    public static final String DOMAIN = "domain";
    public static final String REFERANSENR = "referansenr";


    private static MeterRegistry meterRegistry;
    @Inject
    public DokCounter(MeterRegistry meterRegistry){
        DokCounter.meterRegistry = meterRegistry;
    }

    public static void incrementCounter(Map<String, String> metadata){
        metadata.forEach(DokCounter::incrementCounter);
    }

    public static void incrementCounter(String key, List<String> tags) {
        Counter.builder(DOK_SKANMOTREFERANSENR + key + TOTAL)
                .tags(tags.toArray(new String[0]))
                .register(meterRegistry)
                .increment();
    }

    private static void incrementCounter(String key, String value) {
        Counter.builder(DOK_SKANMOTREFERANSENR + key + TOTAL)
                .tags(key, value)
                .register(meterRegistry)
                .increment();
    }

    public static void incrementError(Throwable throwable){
        Counter.builder(DOK_SKANMOTREFERANSENR + EXCEPTION)
                .tags(ERROR_TYPE, isFunctionalException(throwable) ? FUNCTIONAL_ERROR : TECHNICAL_ERROR)
                .tags(EXCEPTION_NAME, throwable.getClass().getSimpleName())
                .tag(DOMAIN, REFERANSENR)
                .register(meterRegistry)
                .increment();
    }

    private static boolean isFunctionalException(Throwable e) {
        return e instanceof AbstractSkanmotreferansenrFunctionalException;
    }
}
