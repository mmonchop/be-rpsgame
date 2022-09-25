package com.mlmo.rpsgame.service.notification;

import com.mlmo.rpsgame.model.NotificationEvent;
import com.mlmo.rpsgame.model.enums.NotificationEventType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public interface NotificationService {

    void sendNotification(NotificationEvent event, String destination);

    default void sendNotification(String destination, NotificationEventType type,
                                  String id, Map<String, Object> data) {

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .id(id)
                .type(type)
                .eventTime(LocalDateTime.now(ZoneOffset.UTC))
                .data(data)
                .build();

        this.sendNotification(notificationEvent, destination);
    }
}
