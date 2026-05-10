package com.walter.lifelog.web.util;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CouplerWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(CouplerWebSocketHandler.class);
    private static final String SCRIPT_PATH = "coupler/functions/coupler.rb";
    private static final String ATTR_INPUT  = "coupler_input";
    private static final String ATTR_THREAD = "coupler_thread";
    private static final String ATTR_LOCK   = "coupler_lock";
    private static final String DATA_DIR = System.getProperty("user.home") + "/.lifelog/coupler";
    private static final String GETS_PATCH =
            "module Kernel\n" +
            "  def gets(*)\n" +
            "    result = $coupler_input.gets\n" +
            "    $_ = result\n" +
            "    result\n" +
            "  end\n" +
            "end\n";

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        final ReentrantLock lock = new ReentrantLock();
        session.getAttributes().put(ATTR_LOCK, lock);

        final CouplerInputHelper inputHelper = new CouplerInputHelper();
        final WebSocketLineOutputStream wsOut = new WebSocketLineOutputStream(session, lock);
        PrintStream ps;
        try {
            ps = new PrintStream(wsOut, true, "UTF-8");
        } catch (Exception e) {
            log.error("[CouplerWS] PrintStream 생성 실패", e);
            closeQuietly(session);
            return;
        }

        final Thread t = Thread.ofVirtual()
                .name("coupler-" + session.getId().substring(0, 8))
                .start(() -> runScript(session, lock, inputHelper, ps));

        session.getAttributes().put(ATTR_INPUT,  inputHelper);
        session.getAttributes().put(ATTR_THREAD, t);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session,
                                     @NotNull TextMessage message) {
        final CouplerInputHelper inputHelper = (CouplerInputHelper) session.getAttributes().get(ATTR_INPUT);
        if (inputHelper != null) {
            inputHelper.push(message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session,
                                      @NotNull CloseStatus status) {
        cleanup(session);
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session,
                                     @NotNull Throwable exception) throws Exception {
        log.warn("[CouplerWS] transfer error session={}", session.getId(), exception);
        cleanup(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void runScript(WebSocketSession session, ReentrantLock lock,
                           CouplerInputHelper inputHelper, PrintStream ps) {
        ScriptingContainer container = null;
        try {
            final File dataDir = new File(DATA_DIR);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            container = new ScriptingContainer(LocalContextScope.THREADSAFE);
            container.setCurrentDirectory(DATA_DIR);
            container.setOutput(ps);
            container.setError(ps);
            container.put("$coupler_input", inputHelper);
            container.runScriptlet(GETS_PATCH);
            container.runScriptlet(PathType.CLASSPATH, SCRIPT_PATH);
            container.runScriptlet("RandomCoupler.new.run");
            ps.flush();
            sendSafe(session, lock, "__DONE__");
        } catch (Exception e) {
            log.error("[CouplerWS] JRuby error session={}", session.getId(), e);
            sendSafe(session, lock, "__ERROR__: " + e.getMessage());
        } finally {
            if (container != null) {
                try { container.terminate(); } catch (Exception ignored) {}
            }
            closeQuietly(session);
        }
    }

    private void cleanup(WebSocketSession session) {
        final CouplerInputHelper helper = (CouplerInputHelper) session.getAttributes().remove(ATTR_INPUT);
        if (helper != null) {
            helper.close();
        }
        final Thread t = (Thread) session.getAttributes().remove(ATTR_THREAD);
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }

    private void sendSafe(WebSocketSession session, ReentrantLock lock, String text) {
        if (!session.isOpen()) {
            return;
        }
        lock.lock();
        try {
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.warn("[CouplerWS] transfer failure session={}", session.getId(), e);
        } finally {
            lock.unlock();
        }
    }

    private void closeQuietly(WebSocketSession session) {
        if (session.isOpen()) {
            try { session.close(CloseStatus.NORMAL); } catch (Exception ignored) {}
        }
    }
}
