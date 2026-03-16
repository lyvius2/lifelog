package com.walter.lifelog.shared.config.cache;

import com.walter.lifelog.shared.annotation.DynamicCacheable;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class DynamicCacheableScanner implements BeanPostProcessor {
    private final DynamicCacheRegistry dynamicCacheRegistry;

    public DynamicCacheableScanner(DynamicCacheRegistry dynamicCacheRegistry) {
        this.dynamicCacheRegistry = dynamicCacheRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, @NotNull String beanName) throws BeansException {
        ReflectionUtils.doWithMethods(bean.getClass(), method -> {
            final DynamicCacheable annotation = AnnotationUtils.findAnnotation(method, DynamicCacheable.class);
            if (annotation != null) {
                for (String cacheName : annotation.value()) {
                    dynamicCacheRegistry.register(cacheName, annotation.ttlMinutes());
                }
            }
        });
        return bean;
    }
}
