package no.nav.skanmotreferansenr.security;


import no.nav.freg.security.oidc.auth.common.HttpSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public HttpSecurityConfigurer disableCsrfConfigurer() {
		return new HttpSecurityConfigurer() {
			@Override
			public void configure(HttpSecurity http) throws Exception {
				http.csrf().disable();
			}
		};
	}


}
