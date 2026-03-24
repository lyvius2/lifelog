package com.walter.lifelog.web.config;

import com.walter.lifelog.web.service.NotificationBroadcastService;
import com.walter.lifelog.web.util.WebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationBroadcastService notificationBroadcastService;

    public WebSocketConfig(NotificationBroadcastService notificationBroadcastService) {
        this.notificationBroadcastService = notificationBroadcastService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        final WebSocketHandler webSocketHandler = new WebSocketHandler(notificationBroadcastService);
        registry.addHandler(webSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*");
    }
}
