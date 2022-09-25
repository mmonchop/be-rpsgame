package com.mlmo.rpsgame.model;

import com.mlmo.rpsgame.model.enums.NotificationEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String id;
    private NotificationEventType type;
    private Map<String, Object> data;
    private LocalDateTime eventTime;
}
