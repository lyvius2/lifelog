package com.walter.lifelog.worker.config.postgresql

import com.walter.lifelog.worker.util.DatabaseBeanObjectCreator
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.walter.lifelog.worker.log.database.repository"],
    entityManagerFactoryRef = "postgresEntityManagerFactory",
    transactionManagerRef = "postgresTransactionManager",
)
class PostgresJpaConfig(
    private val dbms: PostgresDatabaseProperties,
    private val jpa: PostgresJpaProperties,
    @Value("\${worker.datasource.postgres.ssl.enabled:false}") private val sslEnabled: Boolean,
    @Value("\${worker.datasource.postgres.ssl.mode:verify-full}") private val mode: String,
    @Value("\${worker.datasource.postgres.ssl.root-cert:}") private val rootCert: String,
    @Value("\${worker.datasource.postgres.ssl.cert:}") private val cert: String,
    @Value("\${worker.datasource.postgres.ssl.key:}") private val key: String,
) {

    @Bean
    fun postgresDataSource(): DataSource {
        val dataSource = DatabaseBeanObjectCreator.getDataSource(dbms)
        val hikariDataSource = dataSource as HikariDataSource
        if (sslEnabled) {
            hikariDataSource.addDataSourceProperty("sslmode", mode)
            if (rootCert.isNotBlank()) {
                hikariDataSource.addDataSourceProperty("sslrootcert", rootCert)
            }
            if (cert.isNotBlank()) {
                hikariDataSource.addDataSourceProperty("sslcert", cert)
            }
            if (key.isNotBlank()) {
                hikariDataSource.addDataSourceProperty("sslkey", key)
            }
        }
        return hikariDataSource
    }

    @Bean
    fun postgresEntityManagerFactory(
        @Qualifier("postgresDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean {
        return DatabaseBeanObjectCreator.getEntityManagerFactoryBean(dataSource, jpa, "com.walter.lifelog.worker.log.database.entity", "postgres")
    }

    @Bean
    fun postgresTransactionManager(
        @Qualifier("postgresEntityManagerFactory") factory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(factory)
}