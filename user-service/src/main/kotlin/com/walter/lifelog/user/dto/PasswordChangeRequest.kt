package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "비밀번호 변경 요청. 세 값 모두 /api/auth/public-key 로 받은 공개키로 " +
        "RSA-OAEP(SHA-256) 암호화한 뒤 Base64 로 인코딩해 보냅니다."
)
data class PasswordChangeRequest(
    @Schema(description = "현재 비밀번호 (암호문)", requiredMode = Schema.RequiredMode.REQUIRED)
    val currentPassword: String,

    @Schema(description = "새 비밀번호 (암호문)", requiredMode = Schema.RequiredMode.REQUIRED)
    val newPassword: String,

    @Schema(description = "새 비밀번호 확인 (암호문)", requiredMode = Schema.RequiredMode.REQUIRED)
    val newPasswordConfirm: String,
)
