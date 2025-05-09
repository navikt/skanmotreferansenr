package no.nav.skanmotreferansenr.config.props;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("skanmotreferansenr.vault")
@Validated
public class SkanmotreferansenrVaultProperties {

    @NotBlank
    private String backend;

    @NotBlank
    private String kubernetespath;

}
