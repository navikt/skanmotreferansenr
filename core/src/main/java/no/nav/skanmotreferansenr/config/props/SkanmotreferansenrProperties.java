package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties("skanmotreferansenr")
public class SkanmotreferansenrProperties {

	@NotEmpty
	private String endpointuri;

	@NotEmpty
	private String endpointconfig;

	@NotNull
	private Duration completiontimeout;

	@Valid
	private final Endpoints endpoints = new Endpoints();
	@Valid
	private final Avstem avstem = new Avstem();
	@Valid
	private final FilomraadeProperties filomraade = new FilomraadeProperties();
	@Valid
	private final SftpProperties sftp = new SftpProperties();
	@Valid
	private final JiraConfigProperties jira = new JiraConfigProperties();
	@Valid
	private final Referansenr referansenr = new Referansenr();
	@Valid
	private final SlackProperties slack = new SlackProperties();
	@Valid
	private final Pgp pgp = new Pgp();

	@Data
	public static class Endpoints {

		@NotNull
		private AzureEndpoint foerstesidegenerator;

		@NotNull
		private AzureEndpoint dokarkiv;

	}

	@Data
	public static class Referansenr {
		@NotEmpty
		private String schedule;
	}

	@Data
	public static class Avstem {
		@NotEmpty
		private String schedule;

		private boolean startup;

	}

	@Data
	public static class FilomraadeProperties {

		@NotEmpty
		private String inngaaendemappe;

		@NotEmpty
		private String feilmappe;

		@NotEmpty
		private String avstemmappe;
	}

	@Data
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
	public static class JiraConfigProperties {
		@NotEmpty
		private String username;

		@NotEmpty
		@ToString.Exclude
		private String password;

		@NotEmpty
		private String url;
	}

	@Data
	public static class SlackProperties {
		@NotEmpty
		@ToString.Exclude
		private String token;
		@NotEmpty
		private String channel;
		private boolean enabled;
	}

	@Data
	public static class Pgp {
		/**
		 * passphrase for PGP-tjeneste
		 */
		@NotEmpty
		@ToString.Exclude
		private String passphrase;

		/**
		 * privateKey for PGP-tjeneste
		 */
		@NotEmpty
		private String privateKey;
	}

}