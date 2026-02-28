package com.walter.lifelog.controller.dto

data class PostCategory(
    val categorySeq: Long,
    val categoryName: String,
    var isChecked: Boolean = false
)
