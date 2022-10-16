package com.mlmo.rpsgame.config.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.util.StringUtils;

public final class CustomBearerTokenResolver implements BearerTokenResolver {
    private static final Pattern authorizationPattern = Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$", 2);
    private String bearerTokenHeaderName = "Authorization";

    public CustomBearerTokenResolver() {
        // empty constructor
    }

    public String resolve(final HttpServletRequest request) {
        String authorizationHeaderToken = this.resolveFromAuthorizationHeader(request);
        String parameterToken = this.isParameterTokenSupportedForRequest(request) ? resolveFromRequestParameters(request) : null;
        request.setAttribute("Authentication", "Bearer " + parameterToken);
        if (authorizationHeaderToken != null) {
            if (parameterToken != null) {
                BearerTokenError error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request");
                throw new OAuth2AuthenticationException(error);
            } else {
                return authorizationHeaderToken;
            }
        } else {
            return parameterToken != null ? parameterToken : null;
        }
    }

    private String resolveFromAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader(this.bearerTokenHeaderName);
        if (!StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
            return null;
        } else {
            Matcher matcher = authorizationPattern.matcher(authorization);
            if (!matcher.matches()) {
                BearerTokenError error = BearerTokenErrors.invalidToken("Bearer token is malformed");
                throw new OAuth2AuthenticationException(error);
            } else {
                return matcher.group("token");
            }
        }
    }

    private static String resolveFromRequestParameters(HttpServletRequest request) {
        String[] values = request.getParameterValues("access_token");
        if (values != null && values.length != 0) {
            if (values.length == 1) {
                return values[0];
            } else {
                BearerTokenError error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request");
                throw new OAuth2AuthenticationException(error);
            }
        } else {
            return null;
        }
    }

    private boolean isParameterTokenSupportedForRequest(final HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && "application/x-www-form-urlencoded".equals(request.getContentType()) ||
                "GET".equals(request.getMethod()) ||
                request.getProtocol().startsWith("ws");
    }
}
