package com.sequenceiq.caas.service;

import static com.sequenceiq.caas.util.EncryptionUtil.getHmacKey;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;
import static org.springframework.security.jwt.JwtHelper.encode;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Service;

import com.sequenceiq.caas.util.JsonUtil;
import com.sequenceiq.cloudbreak.client.CaasUser;
import com.sequenceiq.cloudbreak.client.IntrospectRequest;
import com.sequenceiq.cloudbreak.client.IntrospectResponse;
import com.sequenceiq.cloudbreak.client.TokenResponse;

@Service
public class MockCaasService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCaasService.class);

    private static final MacSigner SIGNATURE_VERIFIER = new MacSigner(getHmacKey());

    private static final String LOCATION_HEADER_KEY = "Location";

    private static final String JWT_COOKIE_KEY = "dps-jwt";

    private static final String ISS_KNOX = "KNOXSSO";

    private static final int PLUS_QUANTITY = 1;

    @Inject
    private JsonUtil jsonUtil;

    public CaasUser getUserInfo(@Nonnull HttpServletRequest request) {
        for (Cookie cookie : request.getCookies()) {
            if (JWT_COOKIE_KEY.equals(cookie.getName())) {
                String tokenClaims = decodeAndVerify(cookie.getValue(), SIGNATURE_VERIFIER).getClaims();
                IntrospectResponse introspectResponse = jsonUtil.toObject(tokenClaims, IntrospectResponse.class);
                CaasUser caasUser = new CaasUser();
                caasUser.setName(introspectResponse.getSub());
                caasUser.setPreferredUsername(introspectResponse.getSub());
                caasUser.setTenantId(introspectResponse.getTenantName());
                caasUser.setId(introspectResponse.getTenantName() + '#' + introspectResponse.getSub());
                LOGGER.info(format("Generated caas user: %s", jsonUtil.toJsonString(caasUser)));
                return caasUser;
            }
        }
        throw new NotFoundException("Can not retrieve user from token");
    }

    public IntrospectResponse introSpect(@Nonnull IntrospectRequest encodedToken) {
        try {
            Jwt token = decodeAndVerify(encodedToken.getToken(), SIGNATURE_VERIFIER);
            IntrospectResponse introspectResponse = jsonUtil.toObject(token.getClaims(), IntrospectResponse.class);
            LOGGER.info(String.format("IntrospectResponse: %s", jsonUtil.toJsonString(introspectResponse)));
            return introspectResponse;
        } catch (Exception e) {
            LOGGER.error("Exception in introspect call", e);
            throw new AccessDeniedException("Token is invalid", e);
        }
    }

    public void auth(@Nonnull HttpServletRequest httpServletRequest, @Nonnull HttpServletResponse httpServletResponse, @Nonnull Optional<String> tenant,
            @Nonnull Optional<String> userName, String redirectUri, Boolean active) {
        var host = httpServletRequest.getHeader("Host");
        if (!tenant.isPresent() || !userName.isPresent()) {
            LOGGER.info("redirect to index.html");
            httpServletResponse.setHeader(LOCATION_HEADER_KEY, "login.html?redirect_uri=" + redirectUri);
        } else {
            Cookie cookie = new Cookie(JWT_COOKIE_KEY, getToken(tenant.get(), userName.get(), active).getAccessToken());
            cookie.setDomain(host.split(":")[0]);
            cookie.setPath("/");
            httpServletResponse.addCookie(cookie);
            httpServletResponse.setHeader(LOCATION_HEADER_KEY, redirectUri);
        }
        httpServletResponse.setStatus(SC_FOUND);
    }

    public TokenResponse getTokenResponse(String authorizationCode, String refreshToken) {
        if (authorizationCode != null) {
            return new TokenResponse(authorizationCode, authorizationCode);
        } else if (refreshToken != null) {
            return new TokenResponse(refreshToken, refreshToken);
        } else {
            throw new InvalidParameterException();
        }
    }

    public String authorize(@Nonnull HttpServletResponse httpServletResponse, Optional<String> tenant, Optional<String> userName, Optional<String> redirectUri,
            Boolean active) {
        if (tenant.isPresent() && userName.isPresent()) {
            TokenResponse token = getToken(tenant.get(), userName.get(), active);
            if (redirectUri.isPresent()) {
                LOGGER.info("redirect to " + redirectUri + ".html");
                httpServletResponse.setHeader(LOCATION_HEADER_KEY, redirectUri.get() + "?authorization_code=" + token.getRefreshToken());
                httpServletResponse.setStatus(SC_FOUND);
                return null;
            } else {
                return token.getRefreshToken();
            }
        } else {
            LOGGER.info("redirect to authorize.html");
            httpServletResponse.setHeader(LOCATION_HEADER_KEY, "authorize.html");
            httpServletResponse.setStatus(SC_FOUND);
            return null;
        }
    }

    private TokenResponse getToken(String tenant, String user, boolean active) {
        IntrospectResponse payload = new IntrospectResponse();
        payload.setSub(user);
        payload.setTenantName(tenant);
        payload.setIss(ISS_KNOX);
        payload.setActive(active);
        payload.setExp(Instant.now().plus(PLUS_QUANTITY, DAYS).toEpochMilli());
        String token = encode(jsonUtil.toJsonString(payload), SIGNATURE_VERIFIER).getEncoded();
        LOGGER.info(format("Token generated: %s", token));
        return new TokenResponse(token, token);
    }

}
