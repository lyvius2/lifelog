package com.walter.lifelog.web.consume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walter.lifelog.blog.entity.code.PostStatus;
import com.walter.lifelog.shared.config.messaging.KafkaTopics;
import com.walter.lifelog.web.dto.PostNotification;
import com.walter.lifelog.web.service.NotificationBroadcastService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PostPublishedNotificationConsume {
    private static final Logger log = LoggerFactory.getLogger(PostPublishedNotificationConsume.class);

    private final ObjectMapper objectMapper;
    private final NotificationBroadcastService notificationBroadcastService;

    public PostPublishedNotificationConsume(ObjectMapper objectMapper, NotificationBroadcastService notificationBroadcastService) {
        this.objectMapper = objectMapper;
        this.notificationBroadcastService = notificationBroadcastService;
    }

    @KafkaListener(topics = KafkaTopics.POST_UPDATED)
    public void consume(String message) throws JsonProcessingException {
        final PostNotification postNotification = parsePostNotification(message);
        if (postNotification == null || !PostStatus.PUBLISHED.name().equals(postNotification.status())) {
            return;
        }
        notificationBroadcastService.broadcast(objectMapper.writeValueAsString(postNotification));
    }

    @Nullable
    private PostNotification parsePostNotification(String message) {
        try {
            return objectMapper.convertValue(message, PostNotification.class);
        } catch (Exception e) {
            log.error("failed to parse post notification message: {}", message, e);
            return null;
        }
    }
}
