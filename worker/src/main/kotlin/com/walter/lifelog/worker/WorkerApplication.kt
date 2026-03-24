package com.walter.lifelog.worker

import com.walter.lifelog.worker.config.postgresql.PostgresJpaProperties
import com.walter.lifelog.worker.config.mysql.MysqlDatabaseProperties
import com.walter.lifelog.worker.config.postgresql.PostgresDatabaseProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = ["com.walter.lifelog"],
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
    ],
)
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(
    MysqlDatabaseProperties::class,
    PostgresDatabaseProperties::class,
    PostgresJpaProperties::class,
)
class WorkerApplication

fun main(args: Array<String>) {
    runApplication<WorkerApplication>(*args)
}
