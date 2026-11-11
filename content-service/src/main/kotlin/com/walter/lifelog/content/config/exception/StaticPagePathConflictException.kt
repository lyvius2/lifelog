package com.walter.lifelog.content.config.exception

import com.walter.lifelog.content.dto.StaticPageResponse

/**
 * 이미 쓰이고 있는 경로로 등록을 시도했을 때 던집니다.
 *
 * 단순한 잘못된 입력이 아니라 "덮어쓸지 물어볼 거리" 이므로 400 이 아니라 409 로 응답하고,
 * 어떤 페이지를 덮어쓰게 되는지 알 수 있도록 [existingPage] 를 함께 싣습니다.
 */
class StaticPagePathConflictException(
    val existingPage: StaticPageResponse,
) : RuntimeException("이미 등록된 경로입니다: ${existingPage.path}")
