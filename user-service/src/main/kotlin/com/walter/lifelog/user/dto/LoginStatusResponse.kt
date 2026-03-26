package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 상태 응답")
data class LoginStatusResponse(
    @Schema(description = "로그인 여부", example = "true")
    val isLoggedIn: Boolean,
    @Schema(description = "사용자 이름", example = "Walter")
    val username: String? = null
) {
    companion object {
        @JvmStatic
        fun of(isLoggedIn: Boolean, username: String? = null): LoginStatusResponse {
            return LoginStatusResponse(isLoggedIn, username)
        }
    }
}
