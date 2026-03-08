package com.walter.lifelog.api.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "RSA 공개키 반환 DTO")
data class PublicKeyResponse(
    @Schema(description = "RSA 공개키 (Base64 인코딩)", example = "MIIBIjANBgkqhkiG9w0BAQEFAA...")
    val publicKey: String,
) {
    companion object {
        fun of(publicKey: String): PublicKeyResponse {
            return PublicKeyResponse(publicKey)
        }
    }
}
