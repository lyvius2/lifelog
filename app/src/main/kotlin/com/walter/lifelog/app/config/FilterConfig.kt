package com.walter.lifelog.app.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class FilterConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .formLogin { form -> form.disable() }
            .httpBasic { basic -> basic.disable() }
            .logout { logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler { _: HttpServletRequest,
                                            response: HttpServletResponse,
                                            _: Authentication? ->
                        response.contentType = "application/json"
                        response.characterEncoding = "UTF-8"
                        response.writer.write("""{"success": true, "message": "로그아웃 되었습니다."}""")
                    }
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
            }
            .sessionManagement { session ->
                session
                    .maximumSessions(1)
                    .expiredUrl("/")
            }
            .headers { headers ->
                headers.frameOptions { frame -> frame.sameOrigin() }
            }

        return http.build()
    }
}