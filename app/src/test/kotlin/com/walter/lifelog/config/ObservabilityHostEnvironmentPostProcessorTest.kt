package com.walter.lifelog.config

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.mock.env.MockEnvironment

@DisplayName("ObservabilityHostEnvironmentPostProcessor 테스트")
class ObservabilityHostEnvironmentPostProcessorTest {

    private val sut = ObservabilityHostEnvironmentPostProcessor()

    /** 이 후처리기는 SpringApplication 을 쓰지 않습니다. */
    private val application: SpringApplication = mockk(relaxed = true)

    @Test
    @DisplayName("postProcessEnvironment - LOKI_URL 의 호스트를 관측 호스트로 쓴다")
    fun postProcessEnvironment_lokiUrlGiven_usesItsHost() {
        // given
        val environment = createEnvironment("http://observe.example:3100/loki/api/v1/push")

        // when
        sut.postProcessEnvironment(environment, application)

        // then
        assertThat(environment.getProperty(ObservabilityHostEnvironmentPostProcessor.HOST_PROPERTY))
            .isEqualTo("observe.example")
    }

    @Test
    @DisplayName("postProcessEnvironment - LOKI_URL 이 없으면 localhost 를 쓴다")
    fun postProcessEnvironment_lokiUrlAbsent_fallsBackToLocalhost() {
        // given
        val environment = MockEnvironment()

        // when
        sut.postProcessEnvironment(environment, application)

        // then
        assertThat(environment.getProperty(ObservabilityHostEnvironmentPostProcessor.HOST_PROPERTY))
            .isEqualTo(ObservabilityHostEnvironmentPostProcessor.DEFAULT_HOST)
    }

    @Test
    @DisplayName("postProcessEnvironment - 읽을 수 없는 LOKI_URL 이면 기동을 막지 않고 localhost 로 돌아간다")
    fun postProcessEnvironment_malformedLokiUrl_fallsBackToLocalhost() {
        // given
        val environment = createEnvironment(":// 잘못된 주소")

        // when
        sut.postProcessEnvironment(environment, application)

        // then
        assertThat(environment.getProperty(ObservabilityHostEnvironmentPostProcessor.HOST_PROPERTY))
            .isEqualTo(ObservabilityHostEnvironmentPostProcessor.DEFAULT_HOST)
    }

    @Test
    @DisplayName("postProcessEnvironment - 호스트만 있고 포트가 없는 주소도 호스트를 읽는다")
    fun postProcessEnvironment_urlWithoutPort_readsHost() {
        // given
        val environment = createEnvironment("https://observe.example/loki/api/v1/push")

        // when
        sut.postProcessEnvironment(environment, application)

        // then
        assertThat(environment.getProperty(ObservabilityHostEnvironmentPostProcessor.HOST_PROPERTY))
            .isEqualTo("observe.example")
    }

    @Test
    @DisplayName("postProcessEnvironment - 직접 지정한 관측 호스트가 LOKI_URL 보다 우선한다")
    fun postProcessEnvironment_explicitHost_winsOverLokiUrl() {
        // given
        val environment = createEnvironment("http://observe.example:3100/loki/api/v1/push")
        environment.setProperty(ObservabilityHostEnvironmentPostProcessor.HOST_PROPERTY, "tempo.internal")

        // when
        sut.postProcessEnvironment(environment, application)

        // then
        assertThat(environment.getProperty(ObservabilityHostEnvironmentPostProcessor.HOST_PROPERTY))
            .isEqualTo("tempo.internal")
    }

    @Test
    @DisplayName("postProcessEnvironment - 두 번 호출해도 속성을 중복으로 추가하지 않는다")
    fun postProcessEnvironment_calledTwice_doesNotDuplicateSource() {
        // given
        val environment = createEnvironment("http://observe.example:3100/loki/api/v1/push")

        // when
        sut.postProcessEnvironment(environment, application)
        sut.postProcessEnvironment(environment, application)

        // then
        assertThat(environment.propertySources.count { it.name == "lifelogObservabilityHost" }).isEqualTo(1)
    }

    private fun createEnvironment(lokiUrl: String): MockEnvironment =
        MockEnvironment().withProperty(ObservabilityHostEnvironmentPostProcessor.LOKI_URL_PROPERTY, lokiUrl)
}
