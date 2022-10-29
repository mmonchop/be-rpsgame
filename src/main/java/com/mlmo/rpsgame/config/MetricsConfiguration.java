package com.mlmo.rpsgame.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!insecure")
public class MetricsConfiguration {

    @Value("${info.app.name}")
    private String appName;

    @Value("${spring.profiles.active}")
    private String profile;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> registry.config()
                .commonTags(
                        "app", appName,
                        "env", profile);
    }
}