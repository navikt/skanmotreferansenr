package no.nav.skanmotreferansenr.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("skanmotreferansenr")
@Validated
public class SkanmotreferansenrProperties {

    @NotNull
    private String getmetadatafoerstesideurl;

    @NotNull
    private String stsurl;

    @NotNull
    private ServiceuserProperties serviceuser;
}
