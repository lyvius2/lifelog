package com.walter.lifelog.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PostNotification(
    Long postSeq,
    String title,
    String publishedAt,
    String status
) {
}
