package com.walter.lifelog.shared.config;

import com.walter.lifelog.shared.config.messaging.KafkaTopics;
import com.walter.lifelog.shared.util.KafkaConnectHelper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import java.util.HashMap;

@EnableKafka
@Configuration
public class MessageQueueConfig {
    @Value("${spring.kafka.bootstrap-servers:}")
    private String bootstrapServers;

    @Value("${kafka.ssl.key-location:}")
    private Resource sslKeyResource;

    @Value("${kafka.ssl.certificate-location:}")
    private Resource sslCertResource;

    @Value("${kafka.ssl.ca-location:}")
    private Resource sslCaResource;

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        final HashMap<String, Object> properties = KafkaConnectHelper.getSslProperties(sslKeyResource, sslCertResource, sslCaResource);
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties));
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        final HashMap<String, Object> properties = KafkaConnectHelper.getSslProperties(sslKeyResource, sslCertResource, sslCaResource);
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        final KafkaAdmin admin = new KafkaAdmin(properties);
        admin.setAutoCreate(true);
        return admin;
    }

    @Bean
    public NewTopic topicPostUpdated() {
        return TopicBuilder.name(KafkaTopics.POST_UPDATED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic topicPhotoUpdated() {
        return TopicBuilder.name(KafkaTopics.PHOTO_UPDATED).partitions(2).replicas(1).build();
    }
}