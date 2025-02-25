package no.nav.skanmotreferansenr.avstem;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.journalpostapi.JournalpostConsumer;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.AvstemmingReferanser;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.FeilendeAvstemmingReferanser;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotreferansenr.jira.OpprettJiraService.prettifySummary;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class AvstemService {
	private final JournalpostConsumer journalpostConsumer;

	public AvstemService(JournalpostConsumer journalpostConsumer) {
		this.journalpostConsumer = journalpostConsumer;
	}

	@Handler
	public Set<String> avstemmingsReferanser(Set<String> avstemReferenser) {
		if (isEmpty(avstemReferenser)) {
			return Set.of();
		}

		FeilendeAvstemmingReferanser feilendeAvstemmingReferanser = journalpostConsumer.avstemReferanser(new AvstemmingReferanser(avstemReferenser));
		if (feilendeAvstemmingReferanser == null || isEmpty(feilendeAvstemmingReferanser.referanserIkkeFunnet())) {
			log.info(prettifySummary("Skanmotreferansenr avstemmingsrapport:", avstemReferenser.size(), 0));
			return null;
		}
		log.info("fant {} feilende avstemReferenser", feilendeAvstemmingReferanser.referanserIkkeFunnet());
		return feilendeAvstemmingReferanser.referanserIkkeFunnet();
	}
}
