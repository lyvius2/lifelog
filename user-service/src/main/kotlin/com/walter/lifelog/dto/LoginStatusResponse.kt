package com.walter.lifelog.dto

data class LoginStatusResponse(
    val isLoggedIn: Boolean,
    val username: String? = null
)
