package com.walter.lifelog.api.controller

import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.user.dto.PasswordChangeRequest
import com.walter.lifelog.user.facade.AccountFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자", description = "사용자 계정 관리 API")
@RequestMapping("/api/user")
@RestController
class UserController(
    private val accountFacade: AccountFacade,
) {
    @AdminRequired
    @PostMapping("/change-password")
    @Operation(
        summary = "비밀번호 변경",
        description = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다. 로그인 세션 또는 AccessToken 이 필요하며, " +
            "세 값 모두 /api/auth/public-key 의 공개키로 암호화한 암호문이어야 합니다. " +
            "변경에 성공하면 기존 리프레시 토큰은 모두 폐기됩니다.",
        security = [SecurityRequirement(name = "Authorization")],
    )
    fun changePassword(
        @Parameter(description = "비밀번호 변경 요청 데이터", required = true)
        @RequestBody passwordChangeRequest: PasswordChangeRequest,
        @Parameter(hidden = true) request: HttpServletRequest,
    ): Rest<Unit> {
        val userSeq = request.getAttribute("userSeq") as Long
        accountFacade.changePassword(userSeq, passwordChangeRequest)
        return Rest.ok(Unit)
    }
}
