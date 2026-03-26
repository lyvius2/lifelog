package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 응답")
class LoginResponse(
    @Schema(description = "로그인 성공 여부", example = "true")
    val success: Boolean,
    @Schema(description = "응답 메시지", example = "로그인 성공")
    val message: String,
    @Schema(description = "화면 표시 이름", example = "Walter")
    val displayName: String? = null,
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String? = null,
    @Schema(description = "토큰 만료 시간 (분)", example = "1440")
    val expire: Long? = null
) {
    companion object {
        @JvmStatic
        fun of(displayName: String, accessToken: String): LoginResponse {
            return LoginResponse(
                success = true,
                message = "로그인 성공",
                displayName = displayName,
                accessToken = accessToken,
                expire = 1440L
            )
        }
    }
}