package com.walter.lifelog.user.service

import com.walter.lifelog.shared.util.RsaKeyHolder
import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.dto.PasswordChangeRequest
import com.walter.lifelog.user.dto.UserSimpleInfo
import com.walter.lifelog.user.entity.User
import com.walter.lifelog.user.mapper.UserMapper
import com.walter.lifelog.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

@DisplayName("UserService 테스트")
class UserServiceTest {

    companion object {
        private const val CURRENT_PASSWORD = "currentPw123"
        private const val NEW_PASSWORD = "brandNewPw456"
    }

    private val userRepository: UserRepository = mockk()
    private val userMapper: UserMapper = mockk()
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    private val userService = UserService(userRepository, userMapper, passwordEncoder)

    private fun createUser(
        userSeq: Long = 1L,
        email: String = "admin@lifelog.com",
        name: String = "walter",
        displayName: String = "Walter",
        rawPassword: String = CURRENT_PASSWORD,
    ) = User(
        userSeq = userSeq,
        email = email,
        name = name,
        passwordHash = passwordEncoder.encode(rawPassword)!!,
        displayName = displayName,
        bio = "여행을 좋아하는 개발자",
        profileImageUrl = "https://example.com/profile.jpg",
        githubUrl = "https://github.com/lyvius2",
        linkedinUrl = "https://linkedin.com/in/walter",
    )

    @Test
    @DisplayName("getUserSimpleInfo - 이메일로 사용자를 조회하여 UserSimpleInfo를 반환한다")
    fun getUserSimpleInfo_shouldReturnUserSimpleInfo() {
        // given
        val email = "admin@lifelog.com"
        val user = createUser()
        val expected = UserSimpleInfo(userSeq = 1L, displayName = "Walter", name = "walter")

        every { userRepository.findByEmail(email) } returns user
        every { userMapper.toUserSimpleInfoDto(user) } returns expected

        // when
        val result = userService.getUserSimpleInfo(email)

        // then
        assertThat(result.userSeq).isEqualTo(1L)
        assertThat(result.displayName).isEqualTo("Walter")
        assertThat(result.name).isEqualTo("walter")
        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 1) { userMapper.toUserSimpleInfoDto(user) }
    }

    @Test
    @DisplayName("getUserSimpleInfo - 존재하지 않는 이메일이면 예외가 발생한다")
    fun getUserSimpleInfo_shouldThrowExceptionWhenUserNotFound() {
        // given
        val email = "notfound@lifelog.com"
        every { userRepository.findByEmail(email) } returns null

        // when & then
        assertThatThrownBy { userService.getUserSimpleInfo(email) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User not found with email: $email")
    }

    @Test
    @DisplayName("getAuthorInfoByUserSeq - userSeq로 사용자를 조회하여 Author를 반환한다")
    fun getAuthorInfoByUserSeq_shouldReturnAuthor() {
        // given
        val userSeq = 1L
        val user = createUser()
        val expected = Author(
            name = "Walter",
            bio = "여행을 좋아하는 개발자",
            email = "admin@lifelog.com",
            githubUrl = "https://github.com/lyvius2",
            linkedinUrl = "https://linkedin.com/in/walter",
            profileImageUrl = "https://example.com/profile.jpg",
        )

        every { userRepository.findByUserSeq(userSeq) } returns user
        every { userMapper.toAuthorDto(user) } returns expected

        // when
        val result = userService.getAuthorInfoByUserSeq(userSeq)

        // then
        assertThat(result.name).isEqualTo("Walter")
        assertThat(result.bio).isEqualTo("여행을 좋아하는 개발자")
        assertThat(result.email).isEqualTo("admin@lifelog.com")
        assertThat(result.githubUrl).isEqualTo("https://github.com/lyvius2")
        assertThat(result.linkedinUrl).isEqualTo("https://linkedin.com/in/walter")
        assertThat(result.profileImageUrl).isEqualTo("https://example.com/profile.jpg")
        verify(exactly = 1) { userRepository.findByUserSeq(userSeq) }
        verify(exactly = 1) { userMapper.toAuthorDto(user) }
    }

    @Test
    @DisplayName("getAuthorInfoByUserSeq - 존재하지 않는 userSeq이면 예외가 발생한다")
    fun getAuthorInfoByUserSeq_shouldThrowExceptionWhenUserNotFound() {
        // given
        val userSeq = 999L
        every { userRepository.findByUserSeq(userSeq) } returns null

        // when & then
        assertThatThrownBy { userService.getAuthorInfoByUserSeq(userSeq) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User not found with userSeq: $userSeq")
    }

    /** 서버 공개키로 실제 암호화해 UserService 의 복호화 경로까지 함께 검증합니다. */
    private fun encrypt(plainText: String): String {
        val keyBytes = Base64.getDecoder().decode(RsaKeyHolder.getPublicKeyBase64())
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE, publicKey,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
        )
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray()))
    }

    private fun createPasswordChangeRequest(
        currentPassword: String = CURRENT_PASSWORD,
        newPassword: String = NEW_PASSWORD,
        newPasswordConfirm: String = NEW_PASSWORD,
    ) = PasswordChangeRequest(
        currentPassword = encrypt(currentPassword),
        newPassword = encrypt(newPassword),
        newPasswordConfirm = encrypt(newPasswordConfirm),
    )

    @Test
    @DisplayName("changePassword - 현재 비밀번호가 맞고 새 비밀번호가 일치하면 해시를 갱신한다")
    fun changePassword_validRequest_updatesPasswordHash() {
        // given
        val userSeq = 1L
        every { userRepository.findByUserSeq(userSeq) } returns createUser()
        val hashSlot = slot<String>()
        every { userRepository.updatePassword(userSeq, capture(hashSlot)) } returns 1

        // when
        userService.changePassword(userSeq, createPasswordChangeRequest())

        // then
        verify(exactly = 1) { userRepository.updatePassword(userSeq, any()) }
        assertThat(passwordEncoder.matches(NEW_PASSWORD, hashSlot.captured)).isTrue()
        assertThat(hashSlot.captured).isNotEqualTo(NEW_PASSWORD)
    }

    @Test
    @DisplayName("changePassword - 현재 비밀번호가 틀리면 예외가 발생하고 갱신하지 않는다")
    fun changePassword_wrongCurrentPassword_throwsException() {
        // given
        val userSeq = 1L
        every { userRepository.findByUserSeq(userSeq) } returns createUser()

        // when & then
        assertThatThrownBy {
            userService.changePassword(userSeq, createPasswordChangeRequest(currentPassword = "wrongPw999"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("현재 비밀번호가 일치하지 않습니다.")
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    @DisplayName("changePassword - 새 비밀번호와 확인값이 다르면 예외가 발생한다")
    fun changePassword_confirmMismatch_throwsException() {
        // given
        val userSeq = 1L
        every { userRepository.findByUserSeq(userSeq) } returns createUser()

        // when & then
        assertThatThrownBy {
            userService.changePassword(userSeq, createPasswordChangeRequest(newPasswordConfirm = "otherPw789"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("새 비밀번호가 서로 일치하지 않습니다.")
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    @DisplayName("changePassword - 새 비밀번호가 8자 미만이면 예외가 발생한다")
    fun changePassword_tooShortNewPassword_throwsException() {
        // given
        val userSeq = 1L
        every { userRepository.findByUserSeq(userSeq) } returns createUser()

        // when & then
        assertThatThrownBy {
            userService.changePassword(
                userSeq,
                createPasswordChangeRequest(newPassword = "short7c", newPasswordConfirm = "short7c"),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("새 비밀번호는 8자 이상이어야 합니다.")
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    @DisplayName("changePassword - 새 비밀번호가 현재 비밀번호와 같으면 예외가 발생한다")
    fun changePassword_sameAsCurrentPassword_throwsException() {
        // given
        val userSeq = 1L
        every { userRepository.findByUserSeq(userSeq) } returns createUser()

        // when & then
        assertThatThrownBy {
            userService.changePassword(
                userSeq,
                createPasswordChangeRequest(newPassword = CURRENT_PASSWORD, newPasswordConfirm = CURRENT_PASSWORD),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("새 비밀번호가 현재 비밀번호와 같습니다.")
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    @DisplayName("changePassword - 암호문이 아니면 복호화 실패 메시지로 예외가 발생한다")
    fun changePassword_notEncrypted_throwsException() {
        // given
        val userSeq = 1L
        every { userRepository.findByUserSeq(userSeq) } returns createUser()
        val request = PasswordChangeRequest(CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)

        // when & then
        assertThatThrownBy { userService.changePassword(userSeq, request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("비밀번호를 복호화할 수 없습니다.")
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    @DisplayName("changePassword - 존재하지 않는 userSeq 이면 예외가 발생한다")
    fun changePassword_userNotFound_throwsException() {
        // given
        val userSeq = 999L
        every { userRepository.findByUserSeq(userSeq) } returns null

        // when & then
        assertThatThrownBy { userService.changePassword(userSeq, createPasswordChangeRequest()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User not found with userSeq: $userSeq")
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }
}
