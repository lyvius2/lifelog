package com.walter.lifelog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "게시글 응답 DTO")
public record PostResponse(
    @Schema(description = "게시글 시퀀스", example = "1")
    Long postSeq,

    @Schema(description = "게시글 제목", example = "Spring Boot 시작하기")
    String title,

    @Schema(description = "게시글 요약", example = "Spring Boot를 시작하는 방법을 알아봅니다.")
    String summary,

    @Schema(description = "게시글 내용", example = "# Spring Boot 시작하기\n\n...")
    String content,

    @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    String thumbnailUrl,

    @Schema(description = "조회수", example = "100")
    int viewCount,

    @Schema(description = "추천 게시글 여부", example = "false")
    boolean isFeatured,

    @Schema(description = "발행일시", example = "2026-02-15T10:30:00")
    LocalDateTime publishedAt,

    @Schema(description = "작성자 시퀀스", example = "1")
    Long userSeq,

    @Schema(description = "생성일시", example = "2026-02-10T09:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정일시", example = "2026-02-17T14:20:00")
    LocalDateTime updatedAt
) {
}
