package com.sequenceiq.cloudbreak.client;

import java.util.regex.Pattern;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;

public class CaasClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaasClient.class);

    private static final Pattern LOCATION_PATTERN = Pattern.compile(".*access_token=(.*)\\&expires_in=(\\d*)\\&scope=.*");

    private final String caasDomain;

    private final ConfigKey configKey;

    public CaasClient(String caasDomain, ConfigKey configKey) {
        this.caasDomain = caasDomain;
        this.configKey = configKey;
    }

    public CaasUser getUserInfo(String tenant, String dpsJwtToken) {
        WebTarget caasWebTarget = getCaasWebTarget(tenant);
        WebTarget userInfoWebTarget = caasWebTarget.path("/v0/userinfo");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Cookie", "dps-jwt=" + dpsJwtToken);
        return userInfoWebTarget.request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers)
                .get(CaasUser.class);
    }

    public IntrospectResponse introSpect(String tenant, String dpsJwtToken) {
        WebTarget caasWebTarget = getCaasWebTarget(tenant);
        WebTarget introspectWebTarget = caasWebTarget.path("/v0/introspect");
        return introspectWebTarget.request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new IntrospectRequest(dpsJwtToken)), IntrospectResponse.class);
    }

    private WebTarget getCaasWebTarget(String tenant) {
        if (StringUtils.isNotEmpty(caasDomain)) {
            return RestClientUtil.get(configKey).target("http://" + tenant + '.' + caasDomain);
        } else {
            LOGGER.warn("CAAS isn't configured");
            throw new InvalidTokenException("CAAS isn't configured");
        }
    }
}