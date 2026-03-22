package com.walter.lifelog.worker.config.mysql

import com.walter.lifelog.worker.config.DatabaseProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "worker.datasource.mysql")
data class MysqlDatabaseProperties(
    override var jdbcUrl: String = "",
    override var username: String = "",
    override var password: String = "",
    override var driverClassName: String = "com.mysql.cj.jdbc.Driver",
) : DatabaseProperties