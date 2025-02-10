package no.nav.skanmotreferansenr.config.props;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@Getter
@Setter
@ToString
@ConfigurationProperties("skanmotreferansenr")
@Validated
public class SkanmotreferansenrProperties {

    @NotEmpty
    private String endpointuri;

    @NotEmpty
    private String endpointconfig;

    @NotNull
    private String schedule;

    @NotNull
    private Duration completiontimeout;

    private final Endpoints endpoints = new Endpoints();

    private final ServiceuserProperties serviceuser = new ServiceuserProperties();

    private final FilomraadeProperties filomraade = new FilomraadeProperties();

    private final SftpProperties sftp = new SftpProperties();

    @Data
    @Validated
    public static class Endpoints {

        @NotNull
        private AzureEndpoint foerstesidegenerator;

        @NotNull
        private AzureEndpoint dokarkiv;

    }

    @Getter
    @Setter
    @Validated
    public static class FilomraadeProperties {

        @NotEmpty
        private String inngaaendemappe;

        @NotEmpty
        private String feilmappe;
    }

    @Getter
    @Setter
    @Validated
    public static class ServiceuserProperties {

        @NotEmpty
        private String username;

        @NotEmpty
        @ToString.Exclude
        private String password;

    }

    @Getter
    @Setter
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
}
