package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@ConfigurationProperties("skanmotreferansenr")
@Validated
public class SkanmotreferansenrProperties {

	@NotEmpty
	private String endpointuri;

	@NotEmpty
	private String endpointconfig;

	@NotNull
	private Duration completiontimeout;

	private final Endpoints endpoints = new Endpoints();
	private final Avstem avstem = new Avstem();
	private final ServiceuserProperties serviceuser = new ServiceuserProperties();
	private final FilomraadeProperties filomraade = new FilomraadeProperties();
	private final SftpProperties sftp = new SftpProperties();
	private final JiraProperties jira = new JiraProperties();
	private final Referansenr referansenr = new Referansenr();

	@Data
	@Validated
	public static class Endpoints {

		@NotNull
		private AzureEndpoint foerstesidegenerator;

		@NotNull
		private AzureEndpoint dokarkiv;

	}

	@Data
	@Validated
	public static class Referansenr {
		@NotEmpty
		private String schedule;
	}

	@Data
	@Validated
	public static class Avstem {
		@NotEmpty
		private String schedule;

		private boolean startup;

	}

	@Data
	@Validated
	public static class FilomraadeProperties {

		@NotEmpty
		private String inngaaendemappe;

		@NotEmpty
		private String feilmappe;

		@NotEmpty
		private String avstemmappe;
	}

	@Data
	@Validated
	public static class ServiceuserProperties {

		@NotEmpty
		private String username;

		@NotEmpty
		@ToString.Exclude
		private String password;

	}

	@Data
	@Validated
	public static class SftpProperties {

		@NotNull
		private String host;

		@NotNull
		@ToString.Exclude
		private String privateKey;

		@NotNull
		@ToString.Exclude
		private String hostKey;

		@NotNull
		private String username;

		@NotNull
		private String port;
	}

	@Data
	@Validated
	public static class AzureEndpoint {
		/**
		 * Url til tjeneste som har azure autorisasjon
		 */
		@NotEmpty
		private String url;
		/**
		 * Scope til azure client credential flow
		 */
		@NotEmpty
		private String scope;
	}

	@Data
	@Validated
	public static class JiraProperties {
		@NotEmpty
		private String username;

		@NotEmpty
		private String password;

		@NotEmpty
		private String url;
	}
}
