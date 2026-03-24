package com.walter.lifelog.web.util;

import com.walter.lifelog.web.service.NotificationBroadcastService;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static org.springframework.web.socket.CloseStatus.SERVER_ERROR;

public class WebSocketHandler extends TextWebSocketHandler {
    private final NotificationBroadcastService notificationBroadcastService;

    public WebSocketHandler(NotificationBroadcastService notificationBroadcastService) {
        this.notificationBroadcastService = notificationBroadcastService;
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        notificationBroadcastService.register(session);
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        notificationBroadcastService.unregister(session);
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) throws Exception {
        notificationBroadcastService.unregister(session);
        if (session.isOpen()) {
            session.close(SERVER_ERROR);
        }
    }
}
