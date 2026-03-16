package com.walter.lifelog.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicCacheable {
    String[] value() default {};
    String key() default "";
    long ttlMinutes() default 1440;
}
