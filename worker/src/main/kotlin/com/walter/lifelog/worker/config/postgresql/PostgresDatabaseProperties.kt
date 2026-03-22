package com.walter.lifelog.worker.config.postgresql

import com.walter.lifelog.worker.config.DatabaseProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "worker.datasource.postgres")
data class PostgresDatabaseProperties(
    override var jdbcUrl: String = "",
    override var username: String = "",
    override var password: String = "",
    override var driverClassName: String = "org.postgresql.Driver",
) : DatabaseProperties