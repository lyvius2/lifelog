package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.PublicKeyResponse
import com.walter.lifelog.shared.util.RsaKeyHolder
import com.walter.lifelog.user.dto.LoginRequest
import com.walter.lifelog.user.dto.LoginResponse
import com.walter.lifelog.user.dto.LoginStatusResponse
import com.walter.lifelog.user.dto.RefreshRequest
import com.walter.lifelog.user.facade.AuthFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
              @Parameter(hidden = true) httpRequest: HttpServletRequest,
              @Parameter(hidden = true) httpResponse: HttpServletResponse): ResponseEntity<LoginResponse> {
        val session = httpRequest.getSession(true)
        return ResponseEntity.ok(authFacade.executeAuthenticate(loginRequest, session, httpResponse))
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급합니다. 기존 리프레시 토큰은 무효화됩니다.")
    @PostMapping("/refresh")
    fun refresh(@Parameter(description = "토큰 갱신 요청 데이터", required = true)
                @RequestBody refreshRequest: RefreshRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(authFacade.refreshToken(refreshRequest))
    }

    @Operation(summary = "RSA 공개키 조회", description = "로그인 시 비밀번호 암호화에 사용할 RSA 공개키를 반환합니다.")
    @GetMapping("/public-key")
    fun getPublicKey(): PublicKeyResponse {
        return PublicKeyResponse.of(RsaKeyHolder.getPublicKeyBase64())
    }

    @Operation(summary = "로그아웃", description = "세션과 Redis 캐시를 삭제하고 LIFELOG_SESSION 쿠키를 만료시킵니다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Parameter(hidden = true) httpRequest: HttpServletRequest,
               @Parameter(hidden = true) httpResponse: HttpServletResponse) {
        val session = httpRequest.getSession(false)
        authFacade.executeLogout(session, httpResponse)
    }

    @Operation(summary = "로그인 상태 확인", description = "현재 사용자의 로그인 상태를 확인합니다.")
    @GetMapping("/status")
    fun status(): LoginStatusResponse {
        return authFacade.getLoginStatus()
    }
}