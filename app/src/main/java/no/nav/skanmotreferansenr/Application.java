package no.nav.skanmotreferansenr;

import no.nav.skanmotreferansenr.nais.NaisContract;
import no.nav.skanmotreferansenr.security.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({
        ApplicationConfig.class,
        NaisContract.class,
        SecurityConfig.class
})
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
