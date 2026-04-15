package com.walter.lifelog.user.mapper

import com.walter.lifelog.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers

@DisplayName("UserMapper 테스트")
class UserMapperTest {

    private val userMapper: UserMapper = Mappers.getMapper(UserMapper::class.java)

    private fun createUser(
        userSeq: Long = 1L,
        email: String = "admin@lifelog.com",
        name: String = "walter",
        displayName: String = "Walter",
        bio: String? = "여행을 좋아하는 개발자",
        profileImageUrl: String? = "https://example.com/profile.jpg",
        githubUrl: String? = "https://github.com/lyvius2",
        linkedinUrl: String? = "https://linkedin.com/in/walter",
    ) = User(
        userSeq = userSeq,
        email = email,
        name = name,
        passwordHash = "hashedPassword",
        displayName = displayName,
        bio = bio,
        profileImageUrl = profileImageUrl,
        githubUrl = githubUrl,
        linkedinUrl = linkedinUrl,
    )

    @Test
    @DisplayName("toAuthorDto - User Entity를 Author DTO로 정확하게 매핑한다")
    fun toAuthorDto_shouldMapUserToAuthor() {
        // given
        val user = createUser()

        // when
        val author = userMapper.toAuthorDto(user)

        // then
        assertThat(author).isNotNull
        assertThat(author.name).isEqualTo(user.displayName)
        assertThat(author.bio).isEqualTo(user.bio)
        assertThat(author.email).isEqualTo(user.email)
        assertThat(author.githubUrl).isEqualTo(user.githubUrl)
        assertThat(author.linkedinUrl).isEqualTo(user.linkedinUrl)
        assertThat(author.profileImageUrl).isEqualTo(user.profileImageUrl)
    }

    @Test
    @DisplayName("toAuthorDto - displayName이 Author의 name 필드에 매핑된다")
    fun toAuthorDto_shouldMapDisplayNameToName() {
        // given
        val user = createUser(name = "walter", displayName = "Walter Kim")

        // when
        val author = userMapper.toAuthorDto(user)

        // then
        assertThat(author.name).isEqualTo("Walter Kim")
        assertThat(author.name).isNotEqualTo(user.name)
    }

    @Test
    @DisplayName("toAuthorDto - nullable 필드가 null인 User를 Author로 매핑한다")
    fun toAuthorDto_shouldMapNullableFieldsCorrectly() {
        // given
        val user = createUser(
            bio = null,
            profileImageUrl = null,
            githubUrl = null,
            linkedinUrl = null,
        )

        // when
        val author = userMapper.toAuthorDto(user)

        // then
        assertThat(author).isNotNull
        assertThat(author.name).isEqualTo(user.displayName)
        assertThat(author.email).isEqualTo(user.email)
        assertThat(author.bio).isNull()
        assertThat(author.profileImageUrl).isNull()
        assertThat(author.githubUrl).isNull()
        assertThat(author.linkedinUrl).isNull()
    }

    @Test
    @DisplayName("toUserSimpleInfoDto - User Entity를 UserSimpleInfo DTO로 정확하게 매핑한다")
    fun toUserSimpleInfoDto_shouldMapUserToUserSimpleInfo() {
        // given
        val user = createUser()

        // when
        val userSimpleInfo = userMapper.toUserSimpleInfoDto(user)

        // then
        assertThat(userSimpleInfo).isNotNull
        assertThat(userSimpleInfo.userSeq).isEqualTo(user.userSeq)
        assertThat(userSimpleInfo.displayName).isEqualTo(user.displayName)
        assertThat(userSimpleInfo.name).isEqualTo(user.name)
    }

    @Test
    @DisplayName("toUserSimpleInfoDto - userSeq, name, displayName이 각각 올바른 필드에 매핑된다")
    fun toUserSimpleInfoDto_shouldMapEachFieldCorrectly() {
        // given
        val user = createUser(userSeq = 42L, name = "john", displayName = "John Doe")

        // when
        val userSimpleInfo = userMapper.toUserSimpleInfoDto(user)

        // then
        assertThat(userSimpleInfo.userSeq).isEqualTo(42L)
        assertThat(userSimpleInfo.name).isEqualTo("john")
        assertThat(userSimpleInfo.displayName).isEqualTo("John Doe")
    }
}

