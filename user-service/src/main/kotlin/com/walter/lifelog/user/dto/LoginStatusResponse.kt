package com.walter.lifelog.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 상태 응답")
data class LoginStatusResponse(
    // Kotlin 모듈이 붙으면 Jackson 이 `isLoggedIn` 으로 내보냅니다. 화면은 `loggedIn`
    // 을 읽으므로 이름을 고정합니다 — 모듈 유무에 따라 바뀌면 안 됩니다.
    @get:JsonProperty("loggedIn")
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
