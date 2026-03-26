package com.walter.lifelog.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 작성자 정보")
data class Author(
    @Schema(description = "작성자 이름", example = "Walter")
    val name: String?,
    @Schema(description = "자기소개", example = "여행을 좋아하는 개발자입니다.")
    val bio: String?,
    @Schema(description = "이메일", example = "example@gmail.com")
    val email: String?,
    @Schema(description = "GitHub URL", example = "https://github.com/lyvius2")
    val githubUrl: String?,
    @Schema(description = "LinkedIn URL", example = "https://linkedin.com/in/walter")
    val linkedinUrl: String?,
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val profileImageUrl: String?,
)