package com.mlmo.rpsgame.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.util.Base64;

import static com.mlmo.rpsgame.config.OpenApiConfiguration.BASIC;
import static com.mlmo.rpsgame.config.OpenApiConfiguration.OAUTH2;

@Log
@Component
@ConditionalOnProperty(prefix = "rpsgame.notifications", name = "mode", havingValue = "stomp")
public class WebSocketHttpHeadersBuilder {

    static final String ACCESS_TOKEN = "access_token";

    private final String securityScheme;

    private final String scope;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSocketHttpHeadersBuilder(@Value("${spring.security.scheme}") String securityScheme,
                                       @Value("${spring.security.oauth2.client.token-url:#{null}}") String tokenUrl,
                                       @Value("${spring.security.oauth2.client.client-id:#{null}}") String clientId,
                                       @Value("${spring.security.oauth2.client.client-secret:#{null}}") String clientSecret,
                                       @Value("${spring.security.oauth2.client.scope:#{null}}") String scope) {
        this.securityScheme = securityScheme;
        this.tokenUrl = tokenUrl;
        this.scope = scope;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    WebSocketHttpHeadersBuilder(String securityScheme, String tokenUrl, String clientId, String clientSecret, String scope, RestTemplate restTemplate) {
        this(securityScheme, tokenUrl, clientId, clientSecret, scope);
        this.restTemplate = restTemplate;
    }

    public WebSocketHttpHeaders getWebsocketHttpHeaders(String stompUsername, String stompPassword) {
        return switch (this.securityScheme) {
            case OAUTH2 -> getWebsocketHttpHeadersOauth(stompUsername, stompPassword);
            case BASIC -> getWebsocketHttpHeadersBasicAuth(stompUsername, stompPassword);
            default -> new WebSocketHttpHeaders();
        };
    }

    private WebSocketHttpHeaders getWebsocketHttpHeadersBasicAuth(String stompUsername, String stompPassword) {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        String valueToEncode = stompUsername + ":" + stompPassword;
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes()));
        return headers;
    }

    private WebSocketHttpHeaders getWebsocketHttpHeadersOauth(String stompUsername, String stompPassword) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> passwordCredentialsGrantParams = getPasswordCredentialsGrantParams(stompUsername, stompPassword);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(passwordCredentialsGrantParams, httpHeaders);
            ResponseEntity<String> responseEntity = this.restTemplate.exchange(this.tokenUrl, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(responseEntity.getBody());
            String accessToken = root.get(ACCESS_TOKEN).asText();

            WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
            webSocketHttpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return webSocketHttpHeaders;
        } catch (JsonProcessingException e) {
            throw new AuthenticationServiceException("Error obtaining access token from Azure AD", e);
        }
    }

    private MultiValueMap<String, String> getPasswordCredentialsGrantParams(String stompUsername, String stompPassword) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("client_id", this.clientId);
        params.add("client_secret", this.clientSecret);
        params.add("scope", this.scope);
        params.add("username", stompUsername);
        params.add("password", stompPassword);
        return params;
    }
}
