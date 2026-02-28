package com.walter.lifelog.controller

import com.walter.lifelog.controller.dto.LoginRequest
import com.walter.lifelog.controller.dto.LoginResponse
import com.walter.lifelog.controller.dto.LoginStatusResponse
import com.walter.lifelog.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest, httpRequest: HttpServletRequest): ResponseEntity<LoginResponse> {
        return try {
            ResponseEntity.ok(authService.authenticate(loginRequest, httpRequest))
        } catch (_: BadCredentialsException) {
            ResponseEntity.status(401).body(
                LoginResponse(success = false, message = "이메일 또는 비밀번호가 올바르지 않습니다.")
            )
        } catch (_: Exception) {
            ResponseEntity.status(500).body(
                LoginResponse(success = false, message = "로그인 처리 중 오류가 발생했습니다.")
            )
        }
    }

    @GetMapping("/status")
    fun status(): LoginStatusResponse {
        return authService.getLoginStatus()
    }
}
