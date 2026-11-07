package com.walter.lifelog.shared.event;

import java.time.LocalDateTime;

/**
 * 게시글 저장 직후 발행되는 애플리케이션 이벤트 페이로드.
 * 모듈 간 직접 의존 없이 blog-service 가 발행하고 web 이 수신합니다.
 */
public record PostUpdatedEvent(
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
