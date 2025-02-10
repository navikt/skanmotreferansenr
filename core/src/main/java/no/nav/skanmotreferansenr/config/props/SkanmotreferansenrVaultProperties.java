package no.nav.skanmotreferansenr.config.props;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Data
@ConfigurationProperties("skanmotreferansenr.vault")
@Validated
public class SkanmotreferansenrVaultProperties {

    @NotBlank
    private String backend;

    @NotBlank
    private String kubernetespath;

}
