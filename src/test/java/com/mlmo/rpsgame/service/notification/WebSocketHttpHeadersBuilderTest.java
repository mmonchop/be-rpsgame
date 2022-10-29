package com.mlmo.rpsgame.service.notification;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.util.List;

import static com.mlmo.rpsgame.config.OpenApiConfiguration.OAUTH2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketHttpHeadersBuilderTest {

    private RestTemplate restTemplate;

    private WebSocketHttpHeadersBuilder webSocketHttpHeadersBuilder;

    @BeforeAll
    void init() {
        this.restTemplate = mock(RestTemplate.class);
        this.webSocketHttpHeadersBuilder =
                new WebSocketHttpHeadersBuilder(OAUTH2, "tokenUrl", "clientId", "clientSecret", "scope", this.restTemplate);
    }

    @Test
    @DisplayName("Obtained Oauth Authorization Header")
    void obtainedOauthAccessToken() {
        // Given
        String jwtToken = "jwtToken123";
        String azureADResponseBody = String.format("{\"%s\": \"%s\"}", ACCESS_TOKEN, jwtToken);

        // When
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
          .thenReturn(new ResponseEntity(azureADResponseBody, HttpStatus.OK));

        WebSocketHttpHeaders webSocketHttpHeaders =
                webSocketHttpHeadersBuilder.getWebsocketHttpHeaders("user", "password");

        // Then
        String authorization = (String) ((List) webSocketHttpHeaders.get(HttpHeaders.AUTHORIZATION)).get(0);
        assertThat(authorization, is("Bearer " + jwtToken));
    }

    @Test
    @DisplayName("Raised JsonProcessingException obtaining access token")
    void raisedJsonProcessingException() {
        // Given
        String jwtToken = "jwtToken123";
        String azureADResponseBody = String.format("{\"%s\"}", jwtToken);

        // When
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(new ResponseEntity(azureADResponseBody, HttpStatus.OK));


        Exception exception = assertThrows(RuntimeException.class, () -> webSocketHttpHeadersBuilder.getWebsocketHttpHeaders("user", "password"));

        // When
        String expectedMessage = "Error obtaining access token from Azure AD";
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

}
