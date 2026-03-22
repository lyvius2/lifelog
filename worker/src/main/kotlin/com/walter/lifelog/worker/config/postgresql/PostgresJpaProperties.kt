package com.walter.lifelog.worker.config.postgresql

import com.walter.lifelog.worker.config.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "worker.jpa.postgres")
data class PostgresJpaProperties(
    override var ddlAuto: String = "none",
    override var databasePlatform: String = "org.hibernate.dialect.PostgreSQLDialect",
    override var showSql: Boolean = false,
    override var formatSql: Boolean = false,
) : JpaProperties