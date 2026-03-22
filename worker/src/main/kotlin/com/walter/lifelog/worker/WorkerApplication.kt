package com.walter.lifelog.worker

import com.walter.lifelog.worker.config.mysql.MysqlJpaProperties
import com.walter.lifelog.worker.config.postgresql.PostgresJpaProperties
import com.walter.lifelog.worker.config.mysql.MysqlDatabaseProperties
import com.walter.lifelog.worker.config.postgresql.PostgresDatabaseProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(
    scanBasePackages = ["com.walter.lifelog"],
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
    ],
)
@EnableCaching
@EnableConfigurationProperties(
    MysqlDatabaseProperties::class,
    PostgresDatabaseProperties::class,
    MysqlJpaProperties::class,
    PostgresJpaProperties::class,
)
class WorkerApplication

fun main(args: Array<String>) {
    runApplication<WorkerApplication>(*args)
}
