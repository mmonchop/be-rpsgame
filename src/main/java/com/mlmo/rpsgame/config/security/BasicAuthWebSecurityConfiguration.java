package com.mlmo.rpsgame.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

import static com.mlmo.rpsgame.config.OpenApiConfiguration.BASIC;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@ConditionalOnProperty(prefix = "spring.security", name = "scheme", havingValue = BASIC)
public class BasicAuthWebSecurityConfiguration {

    static final String PLAYER_ROLE = "PLAYER_ROLE";

    static final String[] AUTH_ACTUATOR_WHITELIST = {"/actuator/**"};
    static final String[] AUTH_SWAGGER_UI_WHITELIST = {
            "/v3/api-docs/**", "/swagger-resources/**", "/configuration/**", "/swagger/**",
            "/swagger-ui/**", "/swagger-ui.html", "/webjars/**"};

    @Value("${rpsgame.api-user.username}")
    private String apiUsername;

    @Value("${rpsgame.api-user.password}")
    private String apiPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors().and()
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(AUTH_SWAGGER_UI_WHITELIST).permitAll()
                .antMatchers(AUTH_ACTUATOR_WHITELIST).permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        List<UserDetails> userDetailsList = new ArrayList<>();
        userDetailsList.add(User.withUsername(apiUsername)
                .password(passwordEncoder().encode(apiPassword))
                .roles(PLAYER_ROLE).build());
        return new InMemoryUserDetailsManager(userDetailsList);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
