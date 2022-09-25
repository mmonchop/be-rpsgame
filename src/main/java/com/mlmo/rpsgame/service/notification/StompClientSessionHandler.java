package com.mlmo.rpsgame.service.notification;

import com.mlmo.rpsgame.model.NotificationEvent;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.util.logging.Level;

@Log
@NoArgsConstructor
public class StompClientSessionHandler extends StompSessionHandlerAdapter {

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("CONNECTED to STOMP Message Broker. New session established : " + session.getSessionId());
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        log.log(Level.SEVERE, "Got an exception", exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return NotificationEvent.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        NotificationEvent notificationEvent = (NotificationEvent) payload;
        log.info(String.format("Published Stomp notification [id:%s][type:%s]",
                notificationEvent.getId(), notificationEvent.getType()));
    }

}