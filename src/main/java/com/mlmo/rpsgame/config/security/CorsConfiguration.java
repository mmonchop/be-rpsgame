package com.mlmo.rpsgame.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@ConditionalOnProperty(prefix = "spring.security", name = "mode", havingValue = "basic")
public class CorsConfiguration {

    @Value("${spring.mvc.cors}")
    private String corsMapping;

    @Value("${spring.mvc.allowedOrigins}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).toList());
        config.setAllowedMethods(Stream.of("OPTIONS", "GET", "POST").toList());
        config.setAllowedHeaders(Stream.of("Origin", "Authorization", "Content-Type").toList());
        source.registerCorsConfiguration(corsMapping, config);
        return new CorsFilter(source);
    }

}


