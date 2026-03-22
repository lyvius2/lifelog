package com.walter.lifelog.worker.util

import com.walter.lifelog.worker.config.DatabaseProperties
import com.walter.lifelog.worker.config.JpaProperties
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.util.Properties
import javax.sql.DataSource

class DatabaseBeanObjectCreator {
    companion object {
        @JvmStatic
        fun getDataSource(databaseProperties: DatabaseProperties) : DataSource {
            return DataSourceBuilder.create()
                .type(HikariDataSource::class.java)
                .driverClassName(databaseProperties.driverClassName)
                .url(databaseProperties.jdbcUrl)
                .username(databaseProperties.username)
                .password(databaseProperties.password)
                .build()
        }

        @JvmStatic
        fun getEntityManagerFactoryBean(dataSource: DataSource,
                                        jpaProperties: JpaProperties,
                                        targetPackages: String,
                                        persistenceUnitName: String) : LocalContainerEntityManagerFactoryBean {
            val entityManagerFactoryBean = LocalContainerEntityManagerFactoryBean()
            entityManagerFactoryBean.dataSource = dataSource
            entityManagerFactoryBean.setPackagesToScan(targetPackages)
            entityManagerFactoryBean.persistenceUnitName = persistenceUnitName
            val vendorAdapter = HibernateJpaVendorAdapter()
            vendorAdapter.setDatabasePlatform(jpaProperties.databasePlatform)
            vendorAdapter.setShowSql(jpaProperties.showSql)
            entityManagerFactoryBean.jpaVendorAdapter = vendorAdapter
            val properties = Properties()
            properties["hibernate.hbm2ddl.auto"] = jpaProperties.ddlAuto
            properties["hibernate.format_sql"] = jpaProperties.formatSql.toString()
            entityManagerFactoryBean.setJpaProperties(properties)
            return entityManagerFactoryBean
        }
    }
}