package com.walter.lifelog.dto

class LoginResponse(
    val success: Boolean,
    val message: String,
    val displayName: String? = null,
    val accessToken: String? = null,
    val expire: Long? = null
) {
    companion object {
        @JvmStatic
        fun ok(displayName: String, accessToken: String): LoginResponse {
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