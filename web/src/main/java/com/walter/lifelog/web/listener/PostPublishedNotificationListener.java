package com.walter.lifelog.web.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walter.lifelog.blog.entity.code.PostStatus;
import com.walter.lifelog.shared.event.PostUpdatedEvent;
import com.walter.lifelog.web.dto.PostNotification;
import com.walter.lifelog.web.service.NotificationBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PostPublishedNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(PostPublishedNotificationListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationBroadcastService notificationBroadcastService;

    public PostPublishedNotificationListener(ObjectMapper objectMapper, NotificationBroadcastService notificationBroadcastService) {
        this.objectMapper = objectMapper;
        this.notificationBroadcastService = notificationBroadcastService;
    }

    @EventListener
    public void onPostUpdated(PostUpdatedEvent event) {
        if (!PostStatus.PUBLISHED.name().equals(event.status())) {
            return;
        }
        try {
            notificationBroadcastService.broadcast(objectMapper.writeValueAsString(toNotification(event)));
        } catch (Exception e) {
            log.error("failed to broadcast post published notification — postSeq={}", event.postSeq(), e);
        }
    }

    private PostNotification toNotification(PostUpdatedEvent event) {
        final LocalDateTime publishedAt = event.publishedAt();
        return new PostNotification(event.postSeq(), event.title(), publishedAt == null ? null : publishedAt.toString(), event.status());
    }
}
