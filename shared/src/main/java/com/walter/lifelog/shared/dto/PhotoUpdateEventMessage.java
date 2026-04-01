package com.walter.lifelog.shared.dto;

import java.time.LocalDateTime;

public record PhotoUpdateEventMessage(
    Long photoSeq,
    String filePath,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
