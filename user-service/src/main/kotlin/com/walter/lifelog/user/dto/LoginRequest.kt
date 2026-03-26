package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 요청")
data class LoginRequest(
    @Schema(description = "이메일", example = "admin@lifelog.com", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
    @Schema(description = "비밀번호", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    val password: String
)
