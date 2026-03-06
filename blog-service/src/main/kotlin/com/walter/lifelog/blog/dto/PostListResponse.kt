package com.walter.lifelog.blog.dto

import java.time.LocalDateTime

data class PostListResponse(
    val postSeq: Long,
    val title: String,
    val summary: String?,
    val thumbnailUrl: String?,
    val categoryName: String?,
    val status: String,
    val viewCount: Int,
    val tags: List<String> = emptyList(),
    val publishedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val writerName: String?,
)
