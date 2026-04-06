package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "토큰 갱신 요청")
data class RefreshRequest(
    @Schema(description = "리프레시 토큰", example = "dGhpc0lzQVNlY3VyZVJlZnJlc2hUb2tlbg...")
    val refreshToken: String
)

