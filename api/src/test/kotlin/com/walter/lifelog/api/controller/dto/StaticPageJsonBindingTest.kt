package com.walter.lifelog.api.controller.dto

import com.walter.lifelog.content.dto.StaticPageResponse
import com.walter.lifelog.content.dto.StaticPageScrapeRequest
import com.walter.lifelog.content.entity.code.StaticPageSourceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Kotlin 의 `isPublic` 은 Jackson 기본 규칙으로 `public` 이 됩니다.
 * 요청과 응답에서 이름이 어긋나면 화면이 공개 범위를 반대로 표시하므로 여기서 못박습니다.
 */
@DisplayName("정적 웹페이지 DTO JSON 바인딩 테스트")
class StaticPageJsonBindingTest {

    /* Spring MVC 와 같은 구성입니다 — Jackson 3 + Kotlin 모듈. */
    private val mapper = JsonMapper.builder().addModule(kotlinModule()).build()

    @Test
    @DisplayName("스크랩 요청의 isPublic=false 가 그대로 바인딩된다")
    fun scrapeRequest_isPublicFalse_binds() {
        // given
        val json = """{"url":"https://example.com","path":"p","isPublic":false}"""

        // when
        val request = mapper.readValue(json, StaticPageScrapeRequest::class.java)

        // then
        assertThat(request.isPublic).isFalse()
    }

    @Test
    @DisplayName("스크랩 요청에서 isPublic 을 생략하면 공개가 기본값이다")
    fun scrapeRequest_isPublicOmitted_defaultsToPublic() {
        // given
        val json = """{"url":"https://example.com"}"""

        // when
        val request = mapper.readValue(json, StaticPageScrapeRequest::class.java)

        // then
        assertThat(request.isPublic).isTrue()
        assertThat(request.overwrite).isFalse()
    }

    @Test
    @DisplayName("응답의 JSON 필드명은 public 이 아니라 isPublic 이다")
    fun response_fieldName_isPublic() {
        // given
        val response = StaticPageResponse(
            path = "p",
            url = "/static/p",
            sourceType = StaticPageSourceType.FILE,
            byteSize = 10,
            isPublic = false,
        )

        // when
        val json = mapper.writeValueAsString(response)

        // then
        assertThat(json).contains("\"isPublic\":false")
        assertThat(json).doesNotContain("\"public\"")
    }
}
