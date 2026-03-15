package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.entity.PostTag
import com.walter.lifelog.blog.repository.PostTagsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PostTagService 테스트")
class PostTagServiceTest {

    private val postTagsRepository: PostTagsRepository = mockk(relaxed = true)
    private val postTagService = PostTagService(postTagsRepository)

    @Test
    @DisplayName("getTags - postSeq로 태그 목록을 조회하여 태그 문자열 리스트를 반환한다")
    fun getTags_shouldReturnTagStringList() {
        // given
        val postSeq = 1L
        val postTags = listOf(
            PostTag(postSeq = postSeq, tagSeq = 0, tag = "Spring"),
            PostTag(postSeq = postSeq, tagSeq = 1, tag = "Kotlin"),
            PostTag(postSeq = postSeq, tagSeq = 2, tag = "JPA")
        )
        every { postTagsRepository.findByPostSeq(postSeq) } returns postTags

        // when
        val result = postTagService.getTags(postSeq)

        // then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactly("Spring", "Kotlin", "JPA")
        verify { postTagsRepository.findByPostSeq(postSeq) }
    }

    @Test
    @DisplayName("getTags - 태그가 없으면 빈 리스트를 반환한다")
    fun getTags_shouldReturnEmptyListWhenNoTags() {
        // given
        val postSeq = 99L
        every { postTagsRepository.findByPostSeq(postSeq) } returns emptyList()

        // when
        val result = postTagService.getTags(postSeq)

        // then
        assertThat(result).isEmpty()
        verify { postTagsRepository.findByPostSeq(postSeq) }
    }

    @Test
    @DisplayName("savePostTag - 기존 태그 삭제 후 새 태그를 순서대로 저장한다")
    fun savePostTag_shouldDeleteOldAndSaveNewTags() {
        // given
        val postSeq = 1L
        val postRequest = PostRequest(
            userSeq = 1L,
            categorySeq = 3L,
            title = "테스트",
            summary = "요약",
            status = "DRAFT",
            tags = listOf("Java", "Spring Boot", "Backend")
        )
        val savedTags = mutableListOf<PostTag>()
        every { postTagsRepository.save(capture(savedTags)) } answers { firstArg() }

        // when
        postTagService.savePostTag(postSeq, postRequest)

        // then
        verifyOrder {
            postTagsRepository.deleteByPostSeq(postSeq)
            postTagsRepository.save(any())
        }
        verify(exactly = 3) { postTagsRepository.save(any()) }

        assertThat(savedTags).hasSize(3)
        assertThat(savedTags[0].postSeq).isEqualTo(postSeq)
        assertThat(savedTags[0].tagSeq).isEqualTo(0)
        assertThat(savedTags[0].tag).isEqualTo("Java")
        assertThat(savedTags[1].tagSeq).isEqualTo(1)
        assertThat(savedTags[1].tag).isEqualTo("Spring Boot")
        assertThat(savedTags[2].tagSeq).isEqualTo(2)
        assertThat(savedTags[2].tag).isEqualTo("Backend")
    }

    @Test
    @DisplayName("savePostTag - tags가 null이면 기존 태그만 삭제하고 새 태그는 저장하지 않는다")
    fun savePostTag_shouldOnlyDeleteWhenTagsIsNull() {
        // given
        val postSeq = 1L
        val postRequest = PostRequest(
            userSeq = 1L,
            categorySeq = 3L,
            title = "태그 없는 글",
            summary = "요약",
            status = "DRAFT",
            tags = null
        )

        // when
        postTagService.savePostTag(postSeq, postRequest)

        // then
        verify { postTagsRepository.deleteByPostSeq(postSeq) }
        verify(exactly = 0) { postTagsRepository.save(any()) }
    }

    @Test
    @DisplayName("savePostTag - tags가 빈 리스트면 기존 태그만 삭제하고 새 태그는 저장하지 않는다")
    fun savePostTag_shouldOnlyDeleteWhenTagsIsEmpty() {
        // given
        val postSeq = 1L
        val postRequest = PostRequest(
            userSeq = 1L,
            categorySeq = 3L,
            title = "빈 태그 글",
            summary = "요약",
            status = "DRAFT",
            tags = emptyList()
        )

        // when
        postTagService.savePostTag(postSeq, postRequest)

        // then
        verify { postTagsRepository.deleteByPostSeq(postSeq) }
        verify(exactly = 0) { postTagsRepository.save(any()) }
    }
}

