package com.walter.lifelog.user.service

import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.util.RsaKeyHolder
import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.dto.PasswordChangeRequest
import com.walter.lifelog.user.dto.UserSimpleInfo
import com.walter.lifelog.user.mapper.UserMapper
import com.walter.lifelog.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

    fun getUserSimpleInfo(email: String) : UserSimpleInfo {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("User not found with email: $email")
        return userMapper.toUserSimpleInfoDto(user)
    }

    @DynamicCacheable(value = ["authorInfoByUserSeq"], key = "#userSeq", ttlMinutes = 1440)
    fun getAuthorInfoByUserSeq(userSeq: Long) : Author {
        val user = userRepository.findByUserSeq(userSeq) ?: throw IllegalArgumentException("User not found with userSeq: $userSeq")
        return userMapper.toAuthorDto(user)
    }

    @Transactional
    fun changePassword(userSeq: Long, passwordChangeRequest: PasswordChangeRequest) {
        val user = userRepository.findByUserSeq(userSeq)
            ?: throw IllegalArgumentException("User not found with userSeq: $userSeq")

        val currentPassword = decrypt(passwordChangeRequest.currentPassword)
        val newPassword = decrypt(passwordChangeRequest.newPassword)
        val newPasswordConfirm = decrypt(passwordChangeRequest.newPasswordConfirm)

        require(passwordEncoder.matches(currentPassword, user.passwordHash)) { "현재 비밀번호가 일치하지 않습니다." }
        require(newPassword == newPasswordConfirm) { "새 비밀번호가 서로 일치하지 않습니다." }
        require(newPassword.length >= MIN_PASSWORD_LENGTH) { "새 비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 합니다." }
        require(newPassword != currentPassword) { "새 비밀번호가 현재 비밀번호와 같습니다." }

        val newPasswordHash = checkNotNull(passwordEncoder.encode(newPassword)) { "비밀번호 해시 생성에 실패했습니다." }
        userRepository.updatePassword(userSeq, newPasswordHash)
    }

    /** 복호화 실패는 클라이언트가 보낸 암호문 문제이므로 400 으로 내려보냅니다. */
    private fun decrypt(encrypted: String): String {
        return runCatching { RsaKeyHolder.decrypt(encrypted) }
            .getOrElse { throw IllegalArgumentException("비밀번호를 복호화할 수 없습니다. 페이지를 새로 고친 뒤 다시 시도해주세요.") }
    }
}
