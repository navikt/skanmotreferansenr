package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("slack")
public record SlackProperties(
		@NotEmpty String token,
		@NotEmpty String channel,
		boolean enabled
) {
	@Override
	public String toString() {
		return "SlackProperties{channel='" + channel + "', enabled='" + enabled + "', token=***}}";
	}
}
