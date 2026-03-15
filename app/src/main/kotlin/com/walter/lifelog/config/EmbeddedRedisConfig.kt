package com.walter.lifelog.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import redis.embedded.RedisServer

@Configuration
@Profile("!live")
class EmbeddedRedisConfig {
    private val log = LoggerFactory.getLogger(EmbeddedRedisConfig::class.java)
    private var redisServer: RedisServer? = null

    @PostConstruct
    fun startRedis() {
        try {
            redisServer = RedisServer.newRedisServer().build()
            redisServer?.start()
            log.info("Embedded Redis started")
        } catch (e: Exception) {
            log.error("Embedded Redis started on failed {}", e.message)
        }
    }

    @PreDestroy
    fun stopRedis() {
        try {
            redisServer?.stop()
            log.info("Embedded Redis stopped")
        } catch (e: Exception) {
            log.warn("Embedded Redis stopped on failed {}", e.message)
        }
    }
}

