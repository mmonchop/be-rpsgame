package com.mlmo.rpsgame.controller;

import com.mlmo.rpsgame.model.NotificationEvent;
import lombok.extern.java.Log;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Log
@Controller
@ConditionalOnProperty(prefix = "rpsgame.notifications", name = "mode", havingValue = "stomp")
public class RoomWSController {

    @MessageMapping("/rooms/{roomId}/publish")
    @SendTo("/topic/rooms/{roomId}")
    public NotificationEvent getNotificationEvent(NotificationEvent notificationEvent, @DestinationVariable String roomId) {
        log.info(String.format("Publishing NotificationEvent [id: %s][type: %s] to [/topic/rooms/%s] subscribers.",
                notificationEvent.getId(), notificationEvent.getType(), roomId));
        log.finest(notificationEvent.toString());
        return notificationEvent;
    }
}
