package com.mlmo.rpsgame.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@EnableWebMvc
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Value("${spring.mvc.allowed-origins:}")
    private String allowedOrigins;

    @Value("${spring.mvc.cors:}")
    private String pathPattern;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping(pathPattern).allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

}

