package com.walter.lifelog.content.entity

import com.walter.lifelog.content.entity.code.StaticPageSourceType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * /static/{path} 로 서비스하는 정적 웹페이지 한 건.
 *
 * [html] 은 업로드한 파일이나 스크랩으로 받은 마크업 전문입니다. 크기가 제각각이고
 * 쿼리 대상이 아니며 조회는 항상 [path] 단건이라 MongoDB 에 둡니다.
 *
 * [path] 의 unique index 는 논리 삭제된 문서까지 포함합니다. 같은 경로로 다시 등록하면
 * 새 문서를 만들지 않고 이 문서를 덮어씁니다 (삭제됐던 경로는 그때 되살아납니다).
 *
 * 타임스탬프는 [com.walter.lifelog.content.service.StaticPageService] 가 직접 넣습니다.
 * 이 프로젝트는 `@EnableMongoAuditing` 을 켜지 않아 `@CreatedDate` 류가 동작하지 않습니다.
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

    /** false 면 로그인한 관리자에게만 보입니다. 그 외에는 없는 경로와 똑같이 404 입니다. */
    val isPublic: Boolean = true,

    val isActive: Boolean = true,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val updatedAt: LocalDateTime? = null,
)
