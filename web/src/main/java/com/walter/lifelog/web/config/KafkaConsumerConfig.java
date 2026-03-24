package com.walter.lifelog.web.config;

import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.walter.lifelog.shared.util.KafkaConnectHelper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers:}")
    private String bootstrapServers;

    @Value("${kafka.ssl.key-location:}")
    private Resource sslKeyResource;

    @Value("${kafka.ssl.certificate-location:}")
    private Resource sslCertResource;

    @Value("${kafka.ssl.ca-location:}")
    private Resource sslCaResource;

    @Value("${spring.kafka.consumer.group-id:lifelog-web-group}")
    private String consumerGroupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        final HashMap<String, Object> properties = KafkaConnectHelper.getSslProperties(sslKeyResource, sslCertResource, sslCaResource);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.walter.lifelog.*");
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {
        final ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        return factory;
    }
}
