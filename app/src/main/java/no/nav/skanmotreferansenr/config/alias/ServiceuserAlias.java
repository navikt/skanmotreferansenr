package no.nav.skanmotreferansenr.config.alias;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@ConfigurationProperties("serviceuser")
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class ServiceuserAlias {

    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

}
