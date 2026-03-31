package com.walter.lifelog.shared.config.cache;

import com.walter.lifelog.shared.annotation.DynamicCacheable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class DynamicCacheableInterceptor {
    private static final Logger log = LoggerFactory.getLogger(DynamicCacheableInterceptor.class);

    private final CacheManager cacheManager;
    private final DynamicCacheRegistry dynamicCacheRegistry;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public DynamicCacheableInterceptor(CacheManager cacheManager, DynamicCacheRegistry dynamicCacheRegistry) {
        this.cacheManager = cacheManager;
        this.dynamicCacheRegistry = dynamicCacheRegistry;
    }

    @Around("@annotation(com.walter.lifelog.shared.annotation.DynamicCacheable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        final DynamicCacheable annotation = method.getAnnotation(DynamicCacheable.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        for (String cacheName : annotation.value()) {
            dynamicCacheRegistry.register(cacheName, annotation.ttlMinutes());
        }
        final String cacheKey = resolveCacheKey(joinPoint, method, annotation);
        for (String cacheName : annotation.value()) {
            final Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                try {
                    final Cache.ValueWrapper cached = cache.get(cacheKey);
                    if (cached != null) {
                        return cached.get();
                    }
                } catch (Exception e) {
                    log.warn("Cache deserialization failed for cache='{}', key='{}'. Evicting and refreshing. cause: {}", cacheName, cacheKey, e.getMessage());
                    cache.evict(cacheKey);
                }
            }
        }

        final Object result = joinPoint.proceed();
        if (result != null) {
            for (String cacheName : annotation.value()) {
                final Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.put(cacheKey, result);
                }
            }
        }
        return result;
    }

    private String resolveCacheKey(ProceedingJoinPoint joinPoint, Method method, DynamicCacheable annotation) {
        final String keyExpression = annotation.key();
        if (keyExpression.isEmpty()) {
            return method.getName() + ":" + Arrays.hashCode(joinPoint.getArgs());
        }
        final Object target = joinPoint.getTarget();
        final Object[] args = joinPoint.getArgs();
        final MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, method, args, parameterNameDiscoverer);
        final Object keyValue = parser.parseExpression(keyExpression).getValue(context);
        return keyValue != null ? keyValue.toString() : "null";
    }
}
