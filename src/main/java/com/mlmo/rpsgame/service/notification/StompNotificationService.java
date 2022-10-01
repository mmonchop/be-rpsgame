package com.mlmo.rpsgame.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mlmo.rpsgame.model.NotificationEvent;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Log
@Service
@ConditionalOnProperty(prefix = "rpsgame.notifications", name = "mode", havingValue = "stomp")
public class StompNotificationService implements NotificationService {

    private final String stompBrokerUrl;
    private final WebSocketStompClient stompClient;
    StompSession stompSession;

    @Autowired
    public StompNotificationService(@Value("${rpsgame.notifications.stomp.broker-url:#{null}}") String stompBrokerUrl) {
        this.stompBrokerUrl = stompBrokerUrl;

        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketClient transport = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(transport);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @EventListener(ApplicationReadyEvent.class)
    void init() {
        this.connect(this.stompBrokerUrl);
    }

    public void connect(String stompBrokerUrl) {
        if (StringUtils.isNotEmpty(stompBrokerUrl)) {
            StompClientSessionHandler sessionHandler = new StompClientSessionHandler();
            try {
                stompSession = stompClient.connect(stompBrokerUrl, sessionHandler).get();
            } catch (InterruptedException | ExecutionException e) {
                log.severe("Error connecting to STOMP Message Broker");
                Thread.currentThread().interrupt();
            }
        }
    }

    public void subscribe(String destination, StompFrameHandler stompFrameHandler) {
        stompSession.subscribe(destination, stompFrameHandler);
    }

    @Retryable(value = MessageDeliveryException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000L))
    public synchronized void sendNotification(NotificationEvent notificationEvent, String destination) {
        try {
            Optional.ofNullable(stompSession)
                    .ifPresent(session -> stompSession.send(destination, notificationEvent));

            log.fine(String.format("NOTIFICATION sent: [id: %s], [type: %s], [eventTime: %s]",
                    notificationEvent.getId(), notificationEvent.getType(), formatEventDate(notificationEvent.getEventTime())));
            log.finest(String.format("[notificationEvent: %s]", notificationEvent));
        } catch (IllegalStateException e) {
            log.warning("Has not been possible to send message to STOMP Message Broker");
            connect(this.stompBrokerUrl);
        }
    }

    private String formatEventDate(LocalDateTime eventTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return eventTime.format(formatter);
    }
}
