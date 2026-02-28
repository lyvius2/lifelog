package com.walter.lifelog.controller.dto

data class LoginStatusResponse(
    val isLoggedIn: Boolean,
    val username: String? = null
)
