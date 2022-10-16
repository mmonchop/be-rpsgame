package com.mlmo.rpsgame.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Configuration
@Profile("!insecure")
public class OpenApiConfiguration {

    public static final String API_VERSION = "1.0";
    public static final String BASIC = "basic";
    public static final String OAUTH2 = "oauth2";
    public static final String SECURITY_SCHEME_NAME = "rpsGameSecurityScheme";

    @Value("${info.app.name}")
    private String appName;

    @Value("${info.app.description}")
    private String appDescription;

    @Value("${spring.security.scheme}")
    private String scheme;

    @Value("${spring.security.oauth2.client.auth-url:#{null}}")
    private String authenticationUrl;

    @Value("${spring.security.oauth2.client.token-url:#{null}}")
    private String tokenUrl;

    @Value("${spring.security.oauth2.client.scope:#{null}}")
    private String scope;

    @Bean
    public OpenAPI rpsGameOpenAPI() {
        Info info = new Info()
                .title(appName)
                .description(appDescription)
                .version(API_VERSION);

        SecurityScheme securityScheme =
                switch (scheme) {
                    case BASIC -> getBasicAuthScheme();
                    case OAUTH2 -> getAuthenticationCodeAuthScheme();
                    default -> null;
                };

        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme);

        return new OpenAPI()
                .info(info)
                .components(components);
    }

    private SecurityScheme getBasicAuthScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .scheme(BASIC)
                .in(SecurityScheme.In.HEADER)
                .type(SecurityScheme.Type.HTTP);
    }

    private SecurityScheme getAuthenticationCodeAuthScheme() {
        Scopes scopes = new Scopes().addString(scope, "user.read");

        OAuthFlow authorizationCodeFlow = new OAuthFlow()
                .authorizationUrl(authenticationUrl)
                .tokenUrl(tokenUrl)
                .scopes(scopes);

        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .in(SecurityScheme.In.HEADER).name(AUTHORIZATION)
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(authorizationCodeFlow));
    }
}