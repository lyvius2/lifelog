package com.walter.lifelog.controller.dto

data class PostInputCategory(
    val categorySeq: Long,
    val categoryName: String,
    var isChecked: Boolean = false
)
