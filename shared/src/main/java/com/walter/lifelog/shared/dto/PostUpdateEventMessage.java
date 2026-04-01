package com.walter.lifelog.shared.dto;

import java.time.LocalDateTime;

public record PostUpdateEventMessage(
    Long postSeq,
    Long userSeq,
    Long categorySeq,
    String title,
    String slug,
    String summary,
    String markdownContent,
    String status,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
