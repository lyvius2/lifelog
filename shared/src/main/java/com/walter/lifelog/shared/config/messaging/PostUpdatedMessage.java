package com.walter.lifelog.shared.config.messaging;

import java.time.LocalDateTime;

public record PostUpdatedMessage(
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
) { }
