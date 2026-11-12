package com.walter.lifelog.api.controller.dto

import com.walter.lifelog.user.dto.LoginStatusResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * 화면은 `/api/auth/status` 응답에서 `loggedIn` 을 읽습니다.
 *
 * Kotlin 모듈이 붙으면 Jackson 은 `isLoggedIn` 이라는 이름으로 내보냅니다. 모듈이
 * 없을 때는 `loggedIn` 이 되므로, 의존성 구성이 바뀌면 조용히 이름이 달라집니다.
 * 그렇게 되면 로그인은 되는데 화면만 로그아웃 상태로 보입니다.
 */
@DisplayName("로그인 상태 응답 JSON 바인딩 테스트")
class LoginStatusJsonBindingTest {

    /* Spring MVC 와 같은 구성입니다 — Jackson 3 + Kotlin 모듈. */
    private val mapper = JsonMapper.builder().addModule(kotlinModule()).build()

    @Test
    @DisplayName("로그인 상태는 화면이 읽는 loggedIn 이름으로 나간다")
    fun loginStatus_serializesAsLoggedIn() {
        // given
        val response = LoginStatusResponse.of(true, "Walter")

        // when
        val json = mapper.writeValueAsString(response)

        // then
        assertThat(json).contains("\"loggedIn\":true")
        assertThat(json).doesNotContain("isLoggedIn")
    }

    @Test
    @DisplayName("비로그인 상태도 같은 이름으로 나간다")
    fun loginStatus_loggedOut_serializesAsLoggedIn() {
        // given
        val response = LoginStatusResponse.of(false, "")

        // when
        val json = mapper.writeValueAsString(response)

        // then
        assertThat(json).contains("\"loggedIn\":false")
    }
}
