package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.blog.mapper.PostMapper
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.shared.util.MarkdownConverter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PostService 테스트")
class PostServiceTest {

    private val postsRepository: PostsRepository = mockk()
    private val postMapper: PostMapper = mockk()

    private val postService = PostService(
        postsRepository = postsRepository,
        postMapper = postMapper
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(MarkdownConverter::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(MarkdownConverter::class)
    }

    @Test
    @DisplayName("savePost - PostRequest를 Markdown 변환 후 저장한다")
    fun savePost_shouldConvertMarkdownAndSave() {
        // given
        val userSeq = 1L
        val markdownContent = "## 소개\n\n본문 내용"
        val convertedHtml = "<h2>소개</h2>\n<p>본문 내용</p>"

        val postRequest = PostRequest(
            categorySeq = 3L,
            title = "Spring Boot 시작하기",
            slug = "spring-boot-getting-started",
            summary = "Spring Boot를 시작하는 방법을 알아봅니다.",
            markdownContent = markdownContent,
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            status = "DRAFT",
            tags = listOf("Spring", "Java", "Backend")
        )

        val savedPost = Post(
            postSeq = 10L,
            userSeq = userSeq,
            categorySeq = 3L,
            title = "Spring Boot 시작하기",
            slug = "spring-boot-getting-started",
            summary = "Spring Boot를 시작하는 방법을 알아봅니다.",
            content = convertedHtml,
            markdownContent = markdownContent,
            status = PostStatus.DRAFT,
        )

        every { MarkdownConverter.convert(markdownContent) } returns convertedHtml
        every { postMapper.toEntity(any<PostRequest>()) } returns savedPost.copy(postSeq = null)
        every { postsRepository.save(any()) } returns savedPost

        // when
        val result = postService.savePost(postRequest, userSeq)

        // then
        assertThat(postRequest.userSeq).isEqualTo(userSeq)
        assertThat(postRequest.content).isEqualTo(convertedHtml)
        assertThat(result.postSeq).isEqualTo(10L)

        verify(exactly = 1) { MarkdownConverter.convert(markdownContent) }
        verify(exactly = 1) { postMapper.toEntity(postRequest) }
        verify(exactly = 1) { postsRepository.save(any()) }
    }

    @Test
    @DisplayName("savePost - PUBLISHED 상태일 때 publishedAt이 설정된다")
    fun savePost_shouldSetPublishedAtWhenPublished() {
        // given
        val userSeq = 1L
        val existingPostSeq = 20L
        val markdownContent = "내용"
        val convertedHtml = "<p>내용</p>"

        val postRequest = PostRequest(
            postSeq = existingPostSeq,
            categorySeq = 3L,
            title = "태그 없는 게시글",
            slug = "no-tags-post",
            summary = "태그가 없는 게시글입니다.",
            markdownContent = markdownContent,
            status = "PUBLISHED",
            tags = null
        )

        val savedPost = Post(
            postSeq = existingPostSeq,
            userSeq = userSeq,
            categorySeq = 3L,
            title = "태그 없는 게시글",
            slug = "no-tags-post",
            summary = "태그가 없는 게시글입니다.",
            content = convertedHtml,
            markdownContent = markdownContent,
            status = PostStatus.PUBLISHED,
        )

        every { MarkdownConverter.convert(markdownContent) } returns convertedHtml
        every { postMapper.toEntity(any<PostRequest>()) } returns savedPost
        every { postsRepository.save(any()) } returns savedPost

        // when
        val result = postService.savePost(postRequest, userSeq)

        // then
        assertThat(postRequest.userSeq).isEqualTo(userSeq)
        assertThat(postRequest.content).isEqualTo(convertedHtml)
        assertThat(result.postSeq).isEqualTo(existingPostSeq)
        assertThat(result.publishedAt).isNotNull()

        verify(exactly = 1) { MarkdownConverter.convert(markdownContent) }
        verify(exactly = 1) { postMapper.toEntity(postRequest) }
        verify(exactly = 1) { postsRepository.save(any()) }
    }
}