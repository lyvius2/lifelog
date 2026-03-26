package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.PublicKeyResponse
import com.walter.lifelog.api.facade.AuthFacade
import com.walter.lifelog.shared.util.RsaKeyHolder
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증", description = "로그인/로그아웃 등 인증 관련 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authFacade: AuthFacade,
) {
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 세션을 생성합니다.")
    @PostMapping("/login")
    fun login(@Parameter(description = "로그인 요청 데이터", required = true)
              @RequestBody loginRequest: LoginRequest,
              @Parameter(hidden = true)
              httpRequest: HttpServletRequest): ResponseEntity<LoginResponse> {
        return try {
            val session = httpRequest.getSession(true)
            ResponseEntity.ok(authFacade.executeAuthenticate(loginRequest, session))
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

    @Operation(summary = "RSA 공개키 조회", description = "로그인 시 비밀번호 암호화에 사용할 RSA 공개키를 반환합니다.")
    @GetMapping("/public-key")
    fun getPublicKey(): PublicKeyResponse {
        return PublicKeyResponse.of(RsaKeyHolder.getPublicKeyBase64())
    }

    @Operation(summary = "로그인 상태 확인", description = "현재 사용자의 로그인 상태를 확인합니다.")
    @GetMapping("/status")
    fun status(): LoginStatusResponse {
        return authFacade.getLoginStatus()
    }
}