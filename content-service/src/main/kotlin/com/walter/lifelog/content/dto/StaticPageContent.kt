package com.walter.lifelog.content.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * /static/{path} 를 그려내는 데 필요한 것 전부입니다.
 *
 * 공개 여부를 따로 조회하면 캐시를 두고도 매번 MongoDB 를 읽게 되므로
 * 마크업과 함께 한 덩어리로 캐싱합니다.
 *
 * JSON 이름을 직접 못박습니다. `isPublic` 은 Jackson 기본 규칙으로는 `public` 으로
 * 나가고, live 의 Redis 직렬화기(`RedisSerializer.json()`)에는 Kotlin 모듈이 붙어
 * 있지 않아 되읽을 때 생성자를 찾지 못합니다. @JsonCreator 까지 함께 둬야
 * 모듈 유무와 상관없이 왕복합니다.
 */
data class StaticPageContent @JsonCreator constructor(
    @param:JsonProperty("html")
    val html: String,

    @param:JsonProperty("isPublic")
    @get:JsonProperty("isPublic")
    val isPublic: Boolean,
)
