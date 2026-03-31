package com.walter.lifelog.photo.entity.code

enum class PhotoStatus(
    val statusDescription: String,
) {
    UPLOADED("업로드 직후"),
    PROCESSING("썸네일 처리 중"),
    FAILED("처리 실패"),
    READY("공개 중")
}