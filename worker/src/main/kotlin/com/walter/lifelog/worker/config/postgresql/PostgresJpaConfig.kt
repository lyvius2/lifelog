package com.walter.lifelog.worker.config.postgresql

import com.walter.lifelog.worker.util.DatabaseBeanObjectCreator
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
    basePackages = ["com.walter.lifelog.worker.event.repository"],
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
        val sslProperties = mutableMapOf<String, String>()
        if (sslEnabled) {
            sslProperties["sslmode"] = mode
            if (rootCert.isNotBlank()) sslProperties["sslrootcert"] = rootCert
            if (cert.isNotBlank()) sslProperties["sslcert"] = cert
            if (key.isNotBlank()) sslProperties["sslkey"] = key
        }
        return DatabaseBeanObjectCreator.getDataSource(dbms, sslProperties)
    }

    @Bean
    fun postgresEntityManagerFactory(
        @Qualifier("postgresDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean {
        return DatabaseBeanObjectCreator.getEntityManagerFactoryBean(dataSource, jpa, "com.walter.lifelog.worker.event.entity", "postgres")
    }

    @Bean
    fun postgresTransactionManager(
        @Qualifier("postgresEntityManagerFactory") factory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(factory)
}