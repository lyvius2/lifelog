package com.walter.lifelog.worker.config.mysql

import com.walter.lifelog.worker.util.DatabaseBeanObjectCreator
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class MysqlJooqConfig(
    private val dbms: MysqlDatabaseProperties,
) {
    @Bean
    @Primary
    fun mysqlDataSource(): DataSource {
        return DatabaseBeanObjectCreator.getDataSource(dbms)
    }

    @Bean
    fun mysqlDslContext(
        @Qualifier("mysqlDataSource") dataSource: DataSource,
    ): DSLContext {
        return DSL.using(dataSource, SQLDialect.MYSQL)
    }

    @Bean
    @Primary
    fun mysqlTransactionManager(
        @Qualifier("mysqlDataSource") dataSource: DataSource,
    ): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }
}

