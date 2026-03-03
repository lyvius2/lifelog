package com.walter.lifelog.controller.dto

data class AuthorResponse(
    val name: String?,
    val bio: String?,
    val email: String?,
    val githubUrl: String?,
    val linkedinUrl: String?,
)
