package no.nav.skanmotreferansenr.config.props;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Getter
@Setter
@ToString
@ConfigurationProperties("skanmotreferansenr")
@Validated
public class SkanmotreferansenrProperties {

    @NotNull
    private String getmetadatafoerstesideurl;

    @NotNull
    private String dokarkivurl;

    @NotNull
    private String stsurl;

    @NotEmpty
    private String endpointuri;

    @NotEmpty
    private String endpointconfig;

    @NotNull
    private String schedule;

    @NotNull
    private Duration completiontimeout;

    private final ServiceuserProperties serviceuser = new ServiceuserProperties();

    private final FilomraadeProperties filomraade = new FilomraadeProperties();

    private final SftpProperties sftp = new SftpProperties();

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
}
