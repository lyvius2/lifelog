package com.walter.lifelog.web.config;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class RubyConfig {
    private static final Logger log = LoggerFactory.getLogger(RubyConfig.class);

    @PostConstruct
    public void warmUp() {
        Thread.ofVirtual().name("jruby-warmup").start(() -> {
            log.info("[JRuby] Starting warm-up — pre-initializing JRuby runtime");
            ScriptingContainer warmup = null;
            try {
                warmup = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                warmup.runScriptlet("1 + 1");
                log.info("[JRuby] Warm-up completed successfully");
            } catch (Exception e) {
                log.warn("[JRuby] Warm-up failed — first session may experience startup delay", e);
            } finally {
                if (warmup != null) {
                    try { warmup.terminate(); } catch (Exception ignored) {}
                }
            }
        });
    }
}
