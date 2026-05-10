package com.walter.lifelog.web.config;

import com.walter.lifelog.web.service.NotificationBroadcastService;
import com.walter.lifelog.web.util.CouplerWebSocketHandler;
import com.walter.lifelog.web.util.WebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationBroadcastService notificationBroadcastService;
    private final CouplerWebSocketHandler couplerWebSocketHandler;

    public WebSocketConfig(NotificationBroadcastService notificationBroadcastService, CouplerWebSocketHandler couplerWebSocketHandler) {
        this.notificationBroadcastService = notificationBroadcastService;
        this.couplerWebSocketHandler = couplerWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        final WebSocketHandler webSocketHandler = new WebSocketHandler(notificationBroadcastService);
        registry.addHandler(webSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*");
        registry.addHandler(couplerWebSocketHandler, "/ws/coupler")
                .setAllowedOrigins("*");
    }
}
