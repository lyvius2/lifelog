package com.walter.lifelog.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class NotificationBroadcastService {
    private final Logger log = LoggerFactory.getLogger(NotificationBroadcastService.class);
    private final ConcurrentHashMap<WebSocketSession, ReentrantLock> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session, new ReentrantLock());
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcast(String payload) {
        final List<WebSocketSession> expiredSession = new ArrayList<>();
        for (Map.Entry<WebSocketSession, ReentrantLock> entry : sessions.entrySet()) {
            final WebSocketSession session = entry.getKey();
            final ReentrantLock lock = entry.getValue();
            if (!session.isOpen()) {
                expiredSession.add(session);
                continue;
            }
            lock.lock();

            try {
                session.sendMessage(new TextMessage(payload));
            } catch (Exception e) {
                log.error("send notification error", e);
                expiredSession.add(session);
            } finally {
                lock.unlock();
            }
        }
        expiredSession.forEach(this::unregister);
    }
}
