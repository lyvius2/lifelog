package com.walter.lifelog.blog.dto

data class PostCategory(
    val categorySeq: Long,
    val categoryName: String,
    var isChecked: Boolean = false
)
