package com.walter.lifelog.user.facade

import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.user.dto.PasswordChangeRequest
import com.walter.lifelog.user.service.AuthService
import com.walter.lifelog.user.service.UserService

@Facade
class AccountFacade(
    private val userService: UserService,
    private val authService: AuthService,
) {
    /**
     * 비밀번호를 바꾸고, 바뀐 비밀번호를 모르는 채로 남아 있을 리프레시 토큰을 폐기합니다.
     *
     * 비밀번호 변경은 [UserService.changePassword] 가 자기 트랜잭션에서 커밋합니다.
     * 토큰 폐기는 그 뒤에 실행하므로, 검증에 실패해 변경이 무효가 된 경우에는
     * 로그인이 유지됩니다.
     */
    fun changePassword(userSeq: Long, passwordChangeRequest: PasswordChangeRequest) {
        userService.changePassword(userSeq, passwordChangeRequest)
        authService.revokeRefreshTokens(userSeq)
    }
}
