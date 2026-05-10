package com.walter.lifelog.web.util;

import java.util.concurrent.LinkedBlockingQueue;

public class CouplerInputHelper {
    private static final String CLOSE_SENTINEL = "\u0000";
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private volatile boolean closed = false;

    public void push(String line) {
        if (!closed) {
            queue.offer(line);
        }
    }

    public void close() {
        closed = true;
        queue.offer(CLOSE_SENTINEL);
    }
}
