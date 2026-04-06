package com.walter.lifelog.user.service

import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.dto.UserSimpleInfo
import com.walter.lifelog.user.entity.User
import com.walter.lifelog.user.mapper.UserMapper
import com.walter.lifelog.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UserService 테스트")
class UserServiceTest {

    private val userRepository: UserRepository = mockk()
    private val userMapper: UserMapper = mockk()
    private val userService = UserService(userRepository, userMapper)

    private fun createUser(
        userSeq: Long = 1L,
        email: String = "admin@lifelog.com",
        name: String = "walter",
        displayName: String = "Walter",
    ) = User(
        userSeq = userSeq,
        email = email,
        name = name,
        passwordHash = "hashedPassword",
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
}

