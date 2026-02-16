package com.walter.lifelog.entity.code

enum class PostStatus(
    val statusDescription: String,
) {
    DRAFT("초안:발행대기"),
    PUBLISHED("발행됨"),
    ARCHIVED("보관됨");
}