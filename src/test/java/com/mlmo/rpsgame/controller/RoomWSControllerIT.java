package com.mlmo.rpsgame.controller;

import com.mlmo.rpsgame.model.NotificationEvent;
import com.mlmo.rpsgame.service.notification.StompNotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;


import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class})
@ComponentScan(basePackages = {
        "com.mlmo.rpsgame.controller",
        "com.mlmo.rpsgame.mapper",
        "com.mlmo.rpsgame.service",
        "com.mlmo.rpsgame.repository",
})
class RoomWSControllerIT {

    @LocalServerPort
    private Integer port;

    @Value("${rpsgame.notifications.stomp.rooms-topic-pattern}")
    private String roomsTopicPattern;

    @Autowired
    private StompNotificationService stompNotificationService;

    @BeforeAll
    public void init() {
        stompNotificationService.connect(getWebsocketServicesPath());
    }

    @Test
    @DisplayName("Verify NotificationEvent is sent")
    void verifyNotificationEventSent() {
        // Given
        String roomId = UUID.randomUUID().toString();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        stompNotificationService.subscribe("/topic/rooms/" + roomId, buildStompFrameHandler(countDownLatch));

        // When
        stompNotificationService.sendNotification(getNotificationEvent(), String.format(roomsTopicPattern, roomId));

        // Then
        await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> assertEquals(0, countDownLatch.getCount()));
    }

    private String getWebsocketServicesPath() {
        return String.format("ws://localhost:%d/websocket-services", port);
    }

    private StompFrameHandler buildStompFrameHandler(CountDownLatch countDownLatch) {
        return new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                countDownLatch.countDown();
            }
        };
    }

    private NotificationEvent getNotificationEvent() {
        return NotificationEvent.builder()
                .eventTime(LocalDateTime.now())
                .build();
    }
}
