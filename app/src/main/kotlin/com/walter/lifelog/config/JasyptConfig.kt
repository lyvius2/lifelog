package com.walter.lifelog.config

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties
import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableEncryptableProperties
class JasyptConfig(
    @Value("\${jasypt.encryptor.password:lifelogENC}") private val encryptorPassword: String
) {
    @Bean("jasyptStringEncryptor")
    fun stringEncryptor(): StringEncryptor {
        val config = SimpleStringPBEConfig().apply {
            password = encryptorPassword
            algorithm = "PBEWithMD5AndDES"
            poolSize = 1
        }
        return PooledPBEStringEncryptor().apply { setConfig(config) }
    }
}

