package com.walter.lifelog.worker.config.mysql

import com.walter.lifelog.worker.util.DatabaseBeanObjectCreator
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = ["com.walter.lifelog.blog.repository"],
    entityManagerFactoryRef = "mysqlEntityManagerFactory",
    transactionManagerRef = "mysqlTransactionManager",
)
class MysqlJpaConfig(
    private val dbms: MysqlDatabaseProperties,
    private val jpa: MysqlJpaProperties,
) {

    @Bean
    @Primary
    fun mysqlDataSource(): DataSource {
        return DatabaseBeanObjectCreator.getDataSource(dbms)
    }

    @Bean
    @Primary
    fun mysqlEntityManagerFactory(
        @Qualifier("mysqlDataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean {
        return DatabaseBeanObjectCreator.getEntityManagerFactoryBean(dataSource, jpa, "com.walter.lifelog.blog.entity", "mysql")
    }

    @Bean
    @Primary
    fun mysqlTransactionManager(
        @Qualifier("mysqlEntityManagerFactory") factory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(factory)
}