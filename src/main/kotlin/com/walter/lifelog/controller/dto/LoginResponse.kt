package com.walter.lifelog.controller.dto

class LoginResponse(
    val success: Boolean,
    val message: String,
    val displayName: String? = null
) {
    companion object {
        @JvmStatic
        fun ok(displayName: String): LoginResponse {
            return LoginResponse(true,"로그인 성공", displayName)
        }
    }
}