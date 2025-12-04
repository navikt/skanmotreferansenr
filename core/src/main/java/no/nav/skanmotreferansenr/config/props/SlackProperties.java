package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("slack")
public record SlackProperties(
		@NotBlank String token,
		@NotBlank String channel,
		boolean alertsEnabled
) {
	@Override
	public String toString() {
		return "SlackProperties{channel='" + channel + "', alertsEnabled='" + alertsEnabled + "', token=***}}";
	}
}
