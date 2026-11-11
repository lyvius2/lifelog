package com.walter.lifelog.content.entity

import com.walter.lifelog.content.entity.code.StaticPageSourceType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * /static/{path} 로 서비스하는 정적 웹페이지 한 건.
 *
 * [html] 은 업로드한 파일이나 스크랩으로 받은 마크업 전문입니다. 크기가 제각각이고
 * 쿼리 대상이 아니므로 MongoDB 에 둡니다. 조회는 항상 [path] 단건입니다.
 *
 * [path] 의 unique index 는 논리 삭제된 문서까지 포함합니다. 한 번 쓴 경로는
 * 삭제 후에도 재사용하지 않습니다.
 */
@Document(collection = "static-pages")
data class StaticPage(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val path: String,

    val sourceType: StaticPageSourceType,

    val sourceUrl: String? = null,

    val sourceFileName: String? = null,

    val title: String? = null,

    val html: String,

    val byteSize: Int,

    val userSeq: Long,

    val isActive: Boolean = true,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    val updatedAt: LocalDateTime? = null,
)
