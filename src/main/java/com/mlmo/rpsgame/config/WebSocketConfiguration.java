package com.mlmo.rpsgame.config;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Log
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(prefix = "rpsgame.notifications", name = "mode", havingValue = "stomp")
class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.stomp.endpoint}")
    private String endpoint;

    @Value("#{'${websocket.stomp.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        messageBrokerRegistry.enableSimpleBroker("/topic");
        messageBrokerRegistry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
        log.info(String.format("Stomp endpoint registered for [%s] with the following allowed origins: [%s]", endpoint, allowedOrigins));

        stompEndpointRegistry
                .addEndpoint(endpoint)
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new))
                .withSockJS();
    }

}
