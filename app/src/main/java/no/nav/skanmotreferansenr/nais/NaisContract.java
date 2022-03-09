package no.nav.skanmotreferansenr.nais;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
public class NaisContract {
    private static final String APPLICATION_ALIVE = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready for traffic!";


    private AtomicInteger appStatus = new AtomicInteger(1);

    @Autowired
    public NaisContract(MeterRegistry registry) {
        Gauge.builder("dok_app_is_ready", appStatus, AtomicInteger::get).register(registry);
    }

    @GetMapping("/isAlive")
    public String isAlive() {
        return APPLICATION_ALIVE;
    }

    @RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isReady() {
        appStatus.set(1);

        return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
    }
}
