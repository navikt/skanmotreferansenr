package no.nav.skanmotreferansenr.nais;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.nais.selftest.AbstractDependencyCheck;
import no.nav.skanmotreferansenr.nais.selftest.DependencyCheckResult;
import no.nav.skanmotreferansenr.nais.selftest.Importance;
import no.nav.skanmotreferansenr.nais.selftest.Result;
import no.nav.skanmotreferansenr.nais.selftest.SelftestResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class NaisContract {
	private static final String APPLICATION_ALIVE = "Application is alive!";
	private static final String APPLICATION_READY = "Application is ready for traffic!";
	private static final String APPLICATION_NOT_READY = "Application is not ready for traffic :-(";
	private static AtomicInteger isReady = new AtomicInteger(1);
	private final List<AbstractDependencyCheck> abstractDependencyCheckList;
	private final String appNavn;
	private final String versjon;
	private AtomicInteger app_status = new AtomicInteger();

	@Inject
	public NaisContract(MeterRegistry meterRegistry, List<AbstractDependencyCheck> abstractDependencyCheckList,
                        @Value("${application.name}") String appNavn, @Value("${application.version}") String versjon) {
		Gauge.builder("skanmot_app_is_ready", isReady, AtomicInteger::get).register(meterRegistry);
		this.abstractDependencyCheckList = new ArrayList<>(abstractDependencyCheckList);
		this.appNavn = appNavn;
		this.versjon = versjon;
	}

	@GetMapping("internal/isAlive")
	public String isAlive() {
		return APPLICATION_ALIVE;
	}

	@ResponseBody
	@RequestMapping(value = "internal/isReady", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity isReady() {

		List<DependencyCheckResult> results = new ArrayList<>();

		checkCriticalDependencies(results);

		if (isAnyVitalDependencyUnhealthy(results.stream()
				.map(DependencyCheckResult::getResult)
				.collect(Collectors.toList()))) {
			app_status.set(0);
			return new ResponseEntity<>(APPLICATION_NOT_READY, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		app_status.set(1);

		return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
	}

	@GetMapping("/internal/selftest")
	public @ResponseBody
	SelftestResult selftest() {
		List<DependencyCheckResult> results = new ArrayList<>();
		checkDependencies(results);
		return SelftestResult.builder()
				.appName(appNavn)
				.version(versjon)
				.dependencyCheckResults(results)
				.result(getOverallSelftestResult(results))
				.build();
	}


	private boolean isAnyVitalDependencyUnhealthy(List<Result> results) {
		return results.stream().anyMatch((result) -> result.equals(Result.ERROR));
	}


	private Result getOverallSelftestResult(List<DependencyCheckResult> results) {
		if (results.stream().anyMatch((result) -> result.getResult().equals(Result.ERROR))) {
			return Result.ERROR;
		} else if (results.stream().anyMatch((result) -> result.getResult().equals(Result.WARNING))) {
			return Result.WARNING;
		}

		return Result.OK;
	}

	private void checkCriticalDependencies(List<DependencyCheckResult> results) {

		Flowable.fromIterable(abstractDependencyCheckList)
				.filter(dependency -> dependency.getImportance().equals(Importance.CRITICAL))
				.parallel()
				.runOn(Schedulers.io())
				.map(payload -> payload.check().get())
				.sequential().blockingSubscribe(results::add);
	}

	private void checkDependencies(List<DependencyCheckResult> results) {

		Flowable.fromIterable(abstractDependencyCheckList)
				.parallel()
				.runOn(Schedulers.io())
				.map(payload -> payload.check().get())
				.sequential().blockingSubscribe(results::add);
	}
}
