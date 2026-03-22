package com.walter.lifelog.worker.config.mysql

import com.walter.lifelog.worker.config.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "worker.jpa.mysql")
data class MysqlJpaProperties(
    override var ddlAuto: String = "none",
    override var databasePlatform: String = "org.hibernate.dialect.MySQLDialect",
    override var showSql: Boolean = false,
    override var formatSql: Boolean = false,
) : JpaProperties