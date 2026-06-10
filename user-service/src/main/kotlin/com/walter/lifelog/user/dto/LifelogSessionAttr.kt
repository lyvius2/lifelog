package com.walter.lifelog.user.dto

data class LifelogSessionAttr(
    val userId: String,
    val email: String,
    val username: String,
    val isAdmin: Boolean = true,
)