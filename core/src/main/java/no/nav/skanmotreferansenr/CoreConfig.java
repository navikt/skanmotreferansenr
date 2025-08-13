package no.nav.skanmotreferansenr;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraProperties.JiraServiceUser;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties.JiraConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@ComponentScan
@Configuration
public class CoreConfig {
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

	@Bean
	MethodsClient slackClient(SkanmotreferansenrProperties skanmotreferansenrProperties) {
		return Slack.getInstance().methods(skanmotreferansenrProperties.getSlack().getToken());
	}

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(SkanmotreferansenrProperties properties) {
		return new JiraClient(jiraProperties(properties));
	}

	@Bean
	Clock clock() {
		return Clock.system(DEFAULT_ZONE_ID);
	}

	public JiraProperties jiraProperties(SkanmotreferansenrProperties properties) {
		JiraConfigProperties jira = properties.getJira();

		return JiraProperties.builder()
				.jiraServiceUser(new JiraServiceUser(jira.getUsername(), jira.getPassword()))
				.url(jira.getUrl())
				.build();
	}

}