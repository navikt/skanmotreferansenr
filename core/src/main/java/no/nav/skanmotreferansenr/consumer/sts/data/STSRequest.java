package no.nav.skanmotreferansenr.consumer.sts.data;

import lombok.Getter;

@Getter
public class STSRequest {

    private String grant_type = "client_credentials";
    private String scope = "openid";

}