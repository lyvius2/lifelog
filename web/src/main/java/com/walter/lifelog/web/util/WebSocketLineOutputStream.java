package com.walter.lifelog.web.util;

import org.jspecify.annotations.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

public class WebSocketLineOutputStream extends OutputStream {
    private final WebSocketSession session;
    private final ReentrantLock lock;
    private final StringBuilder buffer = new StringBuilder();

    public WebSocketLineOutputStream(WebSocketSession session, ReentrantLock lock) {
        this.session = session;
        this.lock = lock;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte @NonNull [] b, int off, int len) throws IOException {
        final String chunk = new String(b, off, len, StandardCharsets.UTF_8);
        for (char ch : chunk.toCharArray()) {
            if (ch == '\n') {
                flush();
            } else if (ch != '\r') {
                buffer.append(ch);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        final String line = buffer.toString();
        buffer.setLength(0);
        if (session.isOpen() && !line.isEmpty()) {
            lock.lock();
            try {
                session.sendMessage(new TextMessage(line));
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!buffer.isEmpty()) {
            flush();
        }
    }
}
