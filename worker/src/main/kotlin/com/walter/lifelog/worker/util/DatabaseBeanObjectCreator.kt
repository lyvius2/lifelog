package com.walter.lifelog.worker.util

import com.walter.lifelog.worker.config.DatabaseProperties
import com.walter.lifelog.worker.config.JpaProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.util.Properties
import javax.sql.DataSource

class DatabaseBeanObjectCreator {
    companion object {
        @JvmStatic
        fun getDataSource(databaseProperties: DatabaseProperties, dataSourceProperties: Map<String, String> = emptyMap()) : DataSource {
            val config = HikariConfig()
            config.driverClassName = databaseProperties.driverClassName
            config.jdbcUrl = databaseProperties.jdbcUrl
            config.username = databaseProperties.username
            config.password = databaseProperties.password
            config.maximumPoolSize = 5
            config.minimumIdle = 2
            dataSourceProperties.forEach { (key, value) ->
                config.addDataSourceProperty(key, value)
            }
            return HikariDataSource(config)
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