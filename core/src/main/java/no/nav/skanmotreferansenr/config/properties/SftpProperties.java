package no.nav.skanmotreferansenr.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@Validated
public class SftpProperties {

    @NotEmpty
    private String host;

    @NotEmpty
    private String privateKey;

    @NotEmpty
    private String hostKey;

    @NotEmpty
    private String username;

    @NotEmpty
    private String port;
}
