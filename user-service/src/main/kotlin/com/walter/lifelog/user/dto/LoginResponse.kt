package com.walter.lifelog.user.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.walter.lifelog.shared.util.TokenHandler.EXPIRATION_MILLIS
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
class LoginResponse(
    @Schema(description = "로그인 성공 여부", example = "true")
    val success: Boolean,
    @Schema(description = "응답 메시지", example = "로그인 성공")
    var message: String,
    @Schema(description = "화면 표시 이름", example = "Walter")
    val displayName: String? = null,
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String? = null,
    @Schema(description = "액세스 토큰 유효 시간 단위 (밀리초)", example = "10")
    val accessTokenExpire: Long? = null,
    @Schema(description = "리프레시 토큰", example = "dGhpc0lzQVNlY3VyZVJlZnJlc2hUb2tlbg...")
    val refreshToken: String? = null,
) {
    companion object {
        @JvmStatic
        fun of(displayName: String, message: String, accessToken: String, refreshToken: String): LoginResponse {
            return LoginResponse(
                success = true,
                message = message,
                displayName = displayName,
                accessToken = accessToken,
                accessTokenExpire = EXPIRATION_MILLIS,
                refreshToken = refreshToken
            )
        }
    }
}