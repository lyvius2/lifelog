package com.walter.lifelog.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.time.Duration

@DisplayName("TcpConnectionProbe 테스트")
class TcpConnectionProbeTest {

    private val timeout: Duration = Duration.ofMillis(500)

    @Test
    @DisplayName("of - 포트가 적혀 있으면 그 포트를 쓴다")
    fun of_explicitPort_usesIt() {
        // when
        val probe = TcpConnectionProbe.of("http://observe.example:4318/v1/traces", timeout)

        // then
        assertThat(probe?.toString()).isEqualTo("observe.example:4318")
    }

    @Test
    @DisplayName("of - 포트가 없으면 스킴의 기본 포트를 쓴다")
    fun of_noPort_usesSchemeDefault() {
        // when / then
        assertThat(TcpConnectionProbe.of("http://observe.example/v1/traces", timeout)?.toString())
            .isEqualTo("observe.example:80")
        assertThat(TcpConnectionProbe.of("https://observe.example/v1/traces", timeout)?.toString())
            .isEqualTo("observe.example:443")
    }

    @Test
    @DisplayName("of - 주소가 비어 있으면 null 을 돌려준다")
    fun of_blankUrl_returnsNull() {
        // when / then
        assertThat(TcpConnectionProbe.of(null, timeout)?.toString()).isNull()
        assertThat(TcpConnectionProbe.of("", timeout)?.toString()).isNull()
        assertThat(TcpConnectionProbe.of("   ", timeout)?.toString()).isNull()
    }

    @Test
    @DisplayName("of - 해석할 수 없는 주소면 null 을 돌려준다")
    fun of_unparsableUrl_returnsNull() {
        // when / then
        assertThat(TcpConnectionProbe.of("해석할 수 없는 주소", timeout)?.toString()).isNull()
        assertThat(TcpConnectionProbe.of("/v1/traces", timeout)?.toString()).isNull()
    }

    @Test
    @DisplayName("of - 포트가 없고 스킴의 기본 포트도 알 수 없으면 null 을 돌려준다")
    fun of_unknownSchemeWithoutPort_returnsNull() {
        // when / then
        assertThat(TcpConnectionProbe.of("grpc://observe.example/v1/traces", timeout)?.toString()).isNull()
    }

    @Test
    @DisplayName("invoke - 열려 있는 포트에는 true 를 돌려준다")
    fun invoke_openPort_returnsTrue() {
        // given
        ServerSocket(0).use { server ->
            val probe = TcpConnectionProbe("127.0.0.1", server.localPort, timeout)

            // when / then
            assertThat(probe()).isTrue()
        }
    }

    @Test
    @DisplayName("invoke - 닫혀 있는 포트에는 false 를 돌려준다")
    fun invoke_closedPort_returnsFalse() {
        // given
        val port = ServerSocket(0).use { it.localPort } // 닫은 직후의 포트는 비어 있습니다
        val probe = TcpConnectionProbe("127.0.0.1", port, timeout)

        // when / then
        assertThat(probe()).isFalse()
    }

    @Test
    @DisplayName("invoke - 해석되지 않는 호스트에는 false 를 돌려준다")
    fun invoke_unresolvableHost_returnsFalse() {
        // given
        val probe = TcpConnectionProbe("no-such-host.invalid", 4318, timeout)

        // when / then
        assertThat(probe()).isFalse()
    }
}
