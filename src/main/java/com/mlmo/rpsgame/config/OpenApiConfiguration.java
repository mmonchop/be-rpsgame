package com.mlmo.rpsgame.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

@Configuration
@Profile("!insecure")
public class OpenApiConfiguration {

    public static final String API_VERSION = "1.0";
    public static final String SECURITY_SCHEME_NAME = "rpsGameSecurityScheme";

    @Value("${info.app.name}")
    private String appName;

    @Value("${info.app.description}")
    private String appDescription;

    @Bean
    public OpenAPI rpsGameOpenAPI() {
        Info info = new Info()
                .title(appName)
                .description(appDescription)
                .version(API_VERSION);

        SecurityScheme securityScheme = getBasicAuthScheme();
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme);

        return new OpenAPI()
                .info(info)
                .components(components)
                .security(Arrays.asList(
                        new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
                ));
    }

    private SecurityScheme getBasicAuthScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .scheme("basic")
                .in(SecurityScheme.In.HEADER)
                .type(SecurityScheme.Type.HTTP);
    }

}