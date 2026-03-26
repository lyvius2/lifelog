package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 간략 정보")
data class UserSimpleInfo(
    @Schema(description = "사용자 시퀀스", example = "1")
    val userSeq: Long,
    @Schema(description = "화면 표시 이름", example = "Walter")
    val displayName: String?,
    @Schema(description = "사용자 이름 (고유)", example = "walter")
    val name: String?,
)
