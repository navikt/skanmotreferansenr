package no.nav.skanmotreferansenr.sts;

import lombok.Getter;

@Getter
public class STSRequest {

    private String grant_type = "client_credentials";
    private String scope = "openid";

}