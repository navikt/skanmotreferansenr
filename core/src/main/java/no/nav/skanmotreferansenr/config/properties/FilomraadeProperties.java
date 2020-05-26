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
public class FilomraadeProperties {

    @NotEmpty
    private String inngaaendemappe;

    @NotEmpty
    private String utgaaendemappe;

    @NotEmpty
    private String feilmappe;
}

