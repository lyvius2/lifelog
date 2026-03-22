package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.blog.mapper.PostMapper
import com.walter.lifelog.blog.repository.PostsQueryRepository
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.shared.paging.PageResponse
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
import java.time.LocalDateTime

@DisplayName("PostService 테스트")
class PostServiceTest {

    private val postsRepository: PostsRepository = mockk()
    private val postsQueryRepository: PostsQueryRepository = mockk()
    private val postMapper: PostMapper = mockk()

    private val postService = PostService(
        postsRepository = postsRepository,
        postsQueryRepository = postsQueryRepository,
        postMapper = postMapper,
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
        every { postMapper.toDto(savedPost) } returns PostResponse(
            postSeq = savedPost.postSeq,
            categorySeq = savedPost.categorySeq,
            title = savedPost.title,
            content = savedPost.content,
            markdownContent = savedPost.markdownContent,
            summary = savedPost.summary,
        )

        // when
        val result = postService.savePost(postRequest, userSeq)

        // then
        assertThat(postRequest.userSeq).isEqualTo(userSeq)
        assertThat(postRequest.content).isEqualTo(convertedHtml)
        assertThat(result.postSeq).isEqualTo(10L)

        verify(exactly = 1) { MarkdownConverter.convert(markdownContent) }
        verify(exactly = 1) { postMapper.toEntity(postRequest) }
        verify(exactly = 1) { postsRepository.save(any()) }
        verify(exactly = 1) { postMapper.toDto(savedPost) }
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
        every { postMapper.toDto(savedPost) } returns PostResponse(
            postSeq = savedPost.postSeq,
            categorySeq = savedPost.categorySeq,
            title = savedPost.title,
            content = savedPost.content,
            markdownContent = savedPost.markdownContent,
            summary = savedPost.summary,
            publishedAt = LocalDateTime.now(),
        )

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
        verify(exactly = 1) { postMapper.toDto(savedPost) }
    }

    @Test
    @DisplayName("getSearchedPosts - 검색 조건에 따라 게시글 목록을 페이징하여 반환한다")
    fun getSearchedPosts_shouldReturnPagedPostList() {
        // given
        val postSearchCondition = PostSearchCondition(
            keyword = "Spring",
            categorySeq = 3L,
            status = "PUBLISHED",
            page = 1,
            size = 10,
        )

        val now = LocalDateTime.now()
        val expectedContent = listOf(
            PostListResponse(
                postSeq = 1L,
                title = "Spring Boot 시작하기",
                summary = "Spring Boot를 시작하는 방법을 알아봅니다.",
                thumbnailUrl = "https://example.com/thumb1.jpg",
                categoryName = "Spring",
                status = "PUBLISHED",
                viewCount = 100,
                tags = listOf("Spring", "Java"),
                publishedAt = now.minusDays(1),
                createdAt = now.minusDays(2),
                writerName = "풍우래기",
                writerProfileImage = null,
            ),
            PostListResponse(
                postSeq = 2L,
                title = "Spring Security 적용하기",
                summary = "Spring Security를 적용하는 방법입니다.",
                thumbnailUrl = null,
                categoryName = "Spring",
                status = "PUBLISHED",
                viewCount = 50,
                tags = listOf("Spring", "Security"),
                publishedAt = now.minusDays(3),
                createdAt = now.minusDays(4),
                writerName = "풍우래기",
                writerProfileImage = null,
            ),
        )

        val expectedPageResponse = PageResponse(expectedContent, 1, 10, 2L, 1)
        every { postsQueryRepository.findSearchedPosts(postSearchCondition) } returns expectedPageResponse

        // when
        val result = postService.getSearchedPosts(postSearchCondition)

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.page).isEqualTo(1)
        assertThat(result.size).isEqualTo(10)
        assertThat(result.totalCount).isEqualTo(2L)
        assertThat(result.totalPages).isEqualTo(1)
        assertThat(result.content[0].title).isEqualTo("Spring Boot 시작하기")
        assertThat(result.content[0].categoryName).isEqualTo("Spring")
        assertThat(result.content[1].title).isEqualTo("Spring Security 적용하기")

        verify(exactly = 1) { postsQueryRepository.findSearchedPosts(postSearchCondition) }
    }
}