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
public class ServiceuserProperties {

    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

}
