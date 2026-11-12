package com.walter.lifelog.content.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Web URL 로 정적 웹페이지를 등록하는 요청")
data class StaticPageScrapeRequest(
    @Schema(
        description = "스크랩할 주소. http/https 만 허용하며 응답이 200 이 아니면 거부합니다.",
        example = "https://claude.ai/public/artifacts/xxxxxxxx",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val url: String,

    @Schema(
        description = "원하는 접속 경로. 비워 두면 자동 생성합니다. " +
            "영문 대소문자·숫자·하이픈·언더바 조합으로 최대 20자입니다.",
        example = "my-dashboard",
    )
    val path: String? = null,

    @Schema(
        description = "경로가 이미 쓰이고 있을 때 덮어쓸지 여부. false 면 409 와 함께 기존 페이지 정보를 돌려줍니다.",
        example = "false",
        defaultValue = "false",
    )
    val overwrite: Boolean = false,

    // 요청은 Kotlin 모듈이 생성자 파라미터 이름으로 바인딩합니다. @JsonProperty 를
    // 붙이면 Jackson 3 에서 값이 null 로 넘어와 bind 가 깨집니다.
    @Schema(
        description = "링크가 있으면 누구나 볼 수 있게 할지 여부. false 면 로그인한 관리자에게만 보입니다.",
        example = "true",
        defaultValue = "true",
    )
    val isPublic: Boolean = true,
)
