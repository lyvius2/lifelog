package com.walter.lifelog.config

import com.walter.lifelog.api.config.SessionTtlInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val sessionTtlInterceptor: SessionTtlInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(sessionTtlInterceptor)
            .excludePathPatterns(
                "/actuator/**",
                "/css/**",
                "/js/**",
                "/images/**",
                "/favicon.ico",
                "/webjars/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
            )
    }
}