package com.walter.lifelog.user.dto

data class LoginStatusResponse(
    val isLoggedIn: Boolean,
    val username: String? = null
)
