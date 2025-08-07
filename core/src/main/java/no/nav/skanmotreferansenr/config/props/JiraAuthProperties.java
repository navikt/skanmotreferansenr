package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties("jira")
public record JiraAuthProperties(@NotEmpty String username, @NotEmpty String password) {

	@Override
	public String toString() {
		return "username=" + username + ", password=****";
	}
}

