package com.mlmo.rpsgame.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static com.mlmo.rpsgame.config.OpenApiConfiguration.OAUTH2;
import static com.mlmo.rpsgame.config.security.BasicAuthWebSecurityConfiguration.*;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@ConditionalOnProperty(prefix = "spring.security", name = "scheme", havingValue = OAUTH2)
public class OauthWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(AUTH_SWAGGER_UI_WHITELIST).permitAll()
                .antMatchers(AUTH_ACTUATOR_WHITELIST).permitAll()
                .anyRequest().authenticated();

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthenticationConverter);
    }

}