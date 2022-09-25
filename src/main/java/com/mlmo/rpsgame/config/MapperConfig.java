package com.mlmo.rpsgame.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;

import java.util.Optional;

@Configuration
public class MapperConfig {
    @Bean
    public ObjectMapper objectMapper() {
        final Jackson2ObjectMapperFactoryBean jackson2ObjectMapperFactoryBean = new Jackson2ObjectMapperFactoryBean();
        jackson2ObjectMapperFactoryBean.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        jackson2ObjectMapperFactoryBean.afterPropertiesSet();

        return Optional.ofNullable(jackson2ObjectMapperFactoryBean.getObject())
                .orElse(new ObjectMapper())
                .registerModule(new JavaTimeModule());
    }
}
