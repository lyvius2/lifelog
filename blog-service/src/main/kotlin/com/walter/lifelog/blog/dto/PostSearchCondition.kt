package com.walter.lifelog.blog.dto

data class PostSearchCondition(
    val keyword: String? = null,
    val categorySeq: Long? = null,
    val tag: String? = null,
    val status: String? = null,
    val page: Int = 0,
    val size: Int = 10,
)
