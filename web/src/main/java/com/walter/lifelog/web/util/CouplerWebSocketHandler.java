package com.walter.lifelog.web.util;

import org.apache.commons.lang3.StringUtils;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@Component
public class CouplerWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(CouplerWebSocketHandler.class);
    private static final String ATTR_INPUT     = "coupler_input";
    private static final String ATTR_THREAD    = "coupler_thread";
    private static final String ATTR_LOCK      = "coupler_lock";
    private static final String ATTR_CONTAINER = "coupler_container";
    private static final int MAX_INPUT_LENGTH = 100;
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile("[`|;&<>\\\\]|\\$[({]");
    private static final String GETS_PATCH =
            "module Kernel\n" +
                    "  def gets(*)\n" +
                    "    result = $coupler_input.gets\n" +
                    "    $_ = result\n" +
                    "    result\n" +
                    "  end\n" +
                    "end\n";
    private static final int MAX_SESSIONS = 10;
    private final AtomicInteger activeCount = new AtomicInteger(0);

    @Value("${ruby.execute:false}")
    private boolean isExecuteRuby;
    @Value("${ruby.coupler.path:}")
    private String scriptPath;
    @Value("${ruby.coupler.script:}")
    private String scriptFile;

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws IOException {
        if (activeCount.incrementAndGet() > MAX_SESSIONS) {
            activeCount.decrementAndGet();
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }

        final ReentrantLock lock = new ReentrantLock();
        session.getAttributes().put(ATTR_LOCK, lock);

        if (!isExecuteRuby || StringUtils.isEmpty(scriptFile)) {
            sendSafe(session, lock, "Ruby script execution is disabled on this server.");
            sendSafe(session, lock, "Set 'ruby.execute=true' in application properties to enable it.");
            sendSafe(session, lock, "__DONE__");
            closeQuietly(session);
            return;
        }

        final CouplerInputHelper inputHelper = new CouplerInputHelper();
        final WebSocketLineOutputStream wsOut = new WebSocketLineOutputStream(session, lock);
        PrintStream ps;
        try {
            ps = new PrintStream(wsOut, true, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[CouplerWS] create print-stream failure", e);
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
        if (inputHelper == null) {
            return;
        }

        final String payload = message.getPayload();
        final String violation = validateInput(payload);
        if (violation != null) {
            log.warn("[CouplerWS] input rejected — {} session={}", violation, session.getId());
            final ReentrantLock lock = (ReentrantLock) session.getAttributes().get(ATTR_LOCK);
            sendSafe(session, lock, "[WARN] Input rejected: " + violation);
            return;
        }
        inputHelper.push(payload);
    }

    private String validateInput(String input) {
        if (input == null) {
            return "null input";
        }
        if (input.length() > MAX_INPUT_LENGTH) {
            return "input too long (max " + MAX_INPUT_LENGTH + " chars)";
        }
        if (DANGEROUS_PATTERN.matcher(input).find()) {
            return "disallowed characters detected";
        }
        return null;
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
            final File dataDir = new File(scriptPath);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            final String scriptAbsPath = new File(scriptPath, this.scriptFile).getAbsolutePath();
            container = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
            session.getAttributes().put(ATTR_CONTAINER, container);
            container.setCurrentDirectory(scriptPath);
            container.setOutput(ps);
            container.setError(ps);
            container.put("$coupler_input", inputHelper);
            container.runScriptlet(GETS_PATCH);
            container.runScriptlet(PathType.ABSOLUTE, scriptAbsPath);
            ps.flush();
            sendSafe(session, lock, "__DONE__");
        } catch (Exception e) {
            log.error("[CouplerWS] JRuby error session={}", session.getId(), e);
            sendSafe(session, lock, "__ERROR__: " + e.getMessage());
        } finally {
            terminateContainer(container, session.getId());
            closeQuietly(session);
        }
    }

    private void terminateContainer(ScriptingContainer container, String sessionId) {
        if (container == null) {
            return;
        }
        final Thread t = Thread.ofVirtual()
                .name("coupler-term-" + Math.min(8, sessionId.length()))
                .start(() -> {
                    try { container.terminate(); }
                    catch (Exception ignored) {}
                });
        try {
            t.join(3_000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) {
            log.warn("[CouplerWS] container.terminate() timed out (>3s), abandoned — session={}", sessionId);
        }
    }

    private void cleanup(WebSocketSession session) {
        activeCount.decrementAndGet();
        final ScriptingContainer container = (ScriptingContainer) session.getAttributes().remove(ATTR_CONTAINER);
        if (container != null) {
            Thread.ofVirtual()
                    .name("coupler-term-" + Math.min(8, session.getId().length()))
                    .start(() -> {
                        try { container.terminate(); } catch (Exception ignored) {}
                    });
        }
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
