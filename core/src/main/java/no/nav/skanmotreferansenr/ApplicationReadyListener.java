package no.nav.skanmotreferansenr;

import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Hooks;

import static no.nav.skanmotreferansenr.mdc.MDCConstants.ALL_KEYS;

public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		registerReactorContextPropagation();
	}
	private static void registerReactorContextPropagation() {
		Hooks.enableAutomaticContextPropagation();
		ALL_KEYS.forEach(ApplicationReadyListener::registerMDCKey);
	}

	private static void registerMDCKey(String key) {
		ContextRegistry.getInstance().registerThreadLocalAccessor(
				key,
				() -> MDC.get(key),
				value -> MDC.put(key, value),
				() -> MDC.remove(key)
		);
	}
}
