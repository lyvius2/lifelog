package com.walter.lifelog.shared.dto;

public record PhotoUpdateEventMessage(
    Long photoSeq,
    String filePath,
    String status
) {
}
