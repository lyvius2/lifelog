package com.walter.lifelog.worker.config

import com.walter.lifelog.shared.util.KafkaConnectHelper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer

@Configuration
class KafkaConsumerConfig {
    companion object {
        private val MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors()
    }

    @Value("\${spring.kafka.bootstrap-servers:}")
    private lateinit var bootstrapServers: String

    @Value("\${kafka.ssl.key-location}")
    private lateinit var sslKeyResource: Resource

    @Value("\${kafka.ssl.certificate-location}")
    private lateinit var sslCertResource: Resource

    @Value("\${kafka.ssl.ca-location}")
    private lateinit var sslCaResource: Resource

    @Value("\${spring.kafka.consumer.group-id:lifelog-worker-group}")
    private lateinit var consumerGroupId: String

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val properties = KafkaConnectHelper.getSslProperties(sslKeyResource, sslCertResource, sslCaResource)
        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        properties[ConsumerConfig.GROUP_ID_CONFIG] = consumerGroupId
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonJsonDeserializer::class.java
        properties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        properties[JacksonJsonDeserializer.TRUSTED_PACKAGES] = "com.walter.lifelog.*"
        return DefaultKafkaConsumerFactory(properties)
    }

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, Any>): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.setConsumerFactory(consumerFactory)
        factory.setConcurrency(MAX_CONCURRENCY)
        return factory
    }
}