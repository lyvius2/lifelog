package com.walter.lifelog.user.dto

data class LoginStatusResponse(
    val isLoggedIn: Boolean,
    val username: String? = null
) {
    companion object {
        @JvmStatic
        fun of(isLoggedIn: Boolean, username: String? = null): LoginStatusResponse {
            return LoginStatusResponse(isLoggedIn, username)
        }
    }
}
