package com.walter.lifelog.web.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CouplerInputHelper {
    private static final String CLOSE_SENTINEL = "\u0000";
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private volatile boolean closed = false;

    public void push(String line) {
        if (!closed) {
            queue.offer(line);
        }
    }

    public String gets() {
        if (closed && queue.isEmpty()) {
            return null;
        }
        try {
            final String line = queue.poll(5, TimeUnit.MINUTES);
            if (line == null || CLOSE_SENTINEL.equals(line)) {
                return null;
            }
            return line + "\n";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void close() {
        closed = true;
        queue.offer(CLOSE_SENTINEL);
    }
}
