package no.nav.skanmotreferansenr;

import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.JiraServiceImp;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.skanmotreferansenr.config.props.SkanmotreferansenrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan
@Configuration
public class CoreConfig {

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraServiceImp(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(SkanmotreferansenrProperties properties) {
		return new JiraClient(jiraProperties(properties));
	}

	public JiraProperties jiraProperties(SkanmotreferansenrProperties properties) {
		SkanmotreferansenrProperties.JiraConfigProperties jira = properties.getJira();
		return JiraProperties.builder()
				.jiraServiceUser(new JiraProperties.JiraServiceUser(jira.getUsername(), jira.getPassword()))
				.url(jira.getUrl())
				.build();
	}
}
