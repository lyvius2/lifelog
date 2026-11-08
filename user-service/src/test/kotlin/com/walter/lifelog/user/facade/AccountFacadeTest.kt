package com.walter.lifelog.user.facade

import com.walter.lifelog.user.dto.PasswordChangeRequest
import com.walter.lifelog.user.service.AuthService
import com.walter.lifelog.user.service.UserService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AccountFacade 테스트")
class AccountFacadeTest {

    private val userService: UserService = mockk()
    private val authService: AuthService = mockk()
    private val accountFacade = AccountFacade(userService, authService)

    private fun createPasswordChangeRequest() = PasswordChangeRequest(
        currentPassword = "encryptedCurrent",
        newPassword = "encryptedNew",
        newPasswordConfirm = "encryptedNew",
    )

    @Test
    @DisplayName("changePassword - 비밀번호를 바꾼 뒤 리프레시 토큰을 폐기한다")
    fun changePassword_shouldRevokeRefreshTokensAfterChange() {
        // given
        val userSeq = 1L
        val request = createPasswordChangeRequest()
        every { userService.changePassword(userSeq, request) } just Runs
        every { authService.revokeRefreshTokens(userSeq) } just Runs

        // when
        accountFacade.changePassword(userSeq, request)

        // then
        verifyOrder {
            userService.changePassword(userSeq, request)
            authService.revokeRefreshTokens(userSeq)
        }
    }

    @Test
    @DisplayName("changePassword - 비밀번호 변경이 실패하면 토큰을 폐기하지 않는다")
    fun changePassword_whenChangeFails_shouldNotRevokeRefreshTokens() {
        // given
        val userSeq = 1L
        val request = createPasswordChangeRequest()
        every { userService.changePassword(userSeq, request) } throws
            IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.")

        // when & then
        assertThatThrownBy { accountFacade.changePassword(userSeq, request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("현재 비밀번호가 일치하지 않습니다.")
        verify(exactly = 0) { authService.revokeRefreshTokens(any()) }
    }
}
