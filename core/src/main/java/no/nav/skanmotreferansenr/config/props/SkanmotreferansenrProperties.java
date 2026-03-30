package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import no.nav.dok.validators.Exists;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties("skanmotreferansenr")
public class SkanmotreferansenrProperties {

	@NotBlank
	private String endpointuri;

	@NotBlank
	private String endpointconfig;

	@NotNull
	private Duration completiontimeout;

	@NotBlank
	String slackVarselCron;

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
		@NotBlank
		private String schedule;
	}

	@Data
	public static class Avstem {
		@NotBlank
		private String schedule;

		private boolean startup;

	}

	@Data
	public static class FilomraadeProperties {

		@NotBlank
		private String inngaaendemappe;

		@NotBlank
		private String feilmappe;

		@NotBlank
		private String avstemmappe;
	}

	@Data
	public static class SftpProperties {

		@NotBlank
		private String host;

		@NotBlank
		@Exists
		private String privateKey;

		@NotBlank
		@Exists
		private String hostKey;

		@NotBlank
		private String username;

		@NotBlank
		private String port;
	}

	@Data
	public static class AzureEndpoint {
		/**
		 * Url til tjeneste som har azure autorisasjon
		 */
		@NotBlank
		private String url;
		/**
		 * Scope til azure client credential flow
		 */
		@NotBlank
		private String scope;
	}

	@Data
	public static class JiraConfigProperties {
		@NotBlank
		private String url;
	}

	@Data
	public static class Pgp {
		/**
		 * passphrase for PGP-tjeneste
		 */
		@NotBlank
		@ToString.Exclude
		private String passphrase;

		/**
		 * privateKey for PGP-tjeneste
		 */
		@NotBlank
		@Exists
		private String privateKey;
	}

}
