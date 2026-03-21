package com.walter.lifelog.shared.util;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class KafkaConnectHelper {
    @NotNull
    public static HashMap<String, Object> getSslProperties(Resource sslKeyResource, Resource sslCertResource, Resource sslCaResource) {
        final HashMap<String, Object> properties = new HashMap<>();
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");
        properties.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, readResourceAsString(sslKeyResource));
        properties.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, readResourceAsString(sslCertResource));
        properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
        properties.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, readResourceAsString(sslCaResource));
        return properties;
    }

    @NotNull
    public static String readResourceAsString(Resource resource) {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resource.getFilename(), e);
        }
    }
}
