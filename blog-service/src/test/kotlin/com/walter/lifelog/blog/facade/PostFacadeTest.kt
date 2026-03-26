package com.walter.lifelog.blog.facade

import com.walter.lifelog.blog.dto.*
import com.walter.lifelog.blog.service.CategoryService
import com.walter.lifelog.blog.service.PostService
import com.walter.lifelog.blog.service.PostTagService
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.service.OpenAiChatService
import com.walter.lifelog.shared.service.dto.AiChatRequest
import com.walter.lifelog.shared.util.ViewCountHelper
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.Executors

@DisplayName("PostFacade 테스트")
class PostFacadeTest {

    private val categoryService: CategoryService = mockk()
    private val postService: PostService = mockk()
    private val postTagService: PostTagService = mockk()
    private val openAiChatService: OpenAiChatService = mockk()
    private val virtualThreadExecutor: TaskExecutor = TaskExecutor { task -> Executors.newVirtualThreadPerTaskExecutor().execute(task) }
    private val viewCountHelper: ViewCountHelper = mockk()
    private val transactionTemplate: TransactionTemplate = mockk()

    private val postFacade = PostFacade(
        categoryService, postService, postTagService,
        openAiChatService, virtualThreadExecutor, viewCountHelper, transactionTemplate
    )

    private fun createPostResponse(postSeq: Long = 1L, categorySeq: Long = 10L): PostResponse {
        return PostResponse(
            postSeq = postSeq,
            categorySeq = categorySeq,
            categoryName = "Spring",
            title = "Spring Boot 시작하기",
            summary = "Spring Boot 소개",
            content = "<h5>소개</h5><p>본문</p>",
            markdownContent = "## 소개\n\n본문",
            userSeq = 1L,
            createdAt = LocalDateTime.of(2026, 3, 1, 10, 0),
            tags = listOf("Spring", "Java")
        )
    }

    @Test
    @DisplayName("getPost - inquiryStr로 게시글을 조회하고 태그를 포함한 PostResponse를 반환한다")
    fun getPost_shouldReturnPostResponseWithTags() {
        // given
        val post = createPostResponse()
        every { postService.getPost("1") } returns post
        every { postTagService.getTags(1L) } returns listOf("Spring", "Java")

        // when
        val result = postFacade.getPost("1")

        // then
        assertThat(result.postSeq).isEqualTo(1L)
        assertThat(result.title).isEqualTo("Spring Boot 시작하기")
        assertThat(result.tags).containsExactly("Spring", "Java")
        verify { postService.getPost("1") }
        verify { postTagService.getTags(1L) }
    }

    @Test
    @DisplayName("getPostContents - 게시글과 이전/다음 글 정보, 조회수를 포함한 PostContents를 반환한다")
    fun getPostContents_shouldReturnPostContentsWithPrevNextAndViewCount() {
        // given
        val post = createPostResponse()
        val prevPost = PostSimpleInfo(postSeq = 0L, title = "이전 글")
        val nextPost = PostSimpleInfo(postSeq = 2L, title = "다음 글")

        every { postService.getPost("1") } returns post
        every { postTagService.getTags(1L) } returns listOf("Spring")
        every { postService.getPrevPostInfo(10L, any()) } returns prevPost
        every { postService.getNextPostInfo(10L, any()) } returns nextPost
        every { viewCountHelper.increment("post_1") } returns 42L

        // when
        val result = postFacade.getPostContents("1")

        // then
        assertThat(result.content!!.postSeq).isEqualTo(1L)
        assertThat(result.viewCount).isEqualTo(42L)
        assertThat(result.prevContent!!.title).isEqualTo("이전 글")
        assertThat(result.nextContent!!.title).isEqualTo("다음 글")
        verify { viewCountHelper.increment("post_1") }
    }

    @Test
    @DisplayName("getSearchedPosts - 검색 조건이 null이면 기본 조건으로 검색한다")
    fun getSearchedPosts_withNullCondition_shouldUseDefaultCondition() {
        // given
        val pageResponse = PageResponse<PostListResponse>(emptyList(), 1, 10, 0, 0)
        every { postService.getSearchedPosts(any()) } returns pageResponse

        // when
        val result = postFacade.getSearchedPosts(null)

        // then
        assertThat(result.content).isEmpty()
        verify { postService.getSearchedPosts(any()) }
    }

    @Test
    @DisplayName("getSearchedPosts - 검색 조건이 있으면 해당 조건으로 검색한다")
    fun getSearchedPosts_withCondition_shouldSearchWithGivenCondition() {
        // given
        val condition = PostSearchCondition(keyword = "Spring", page = 1)
        val pageResponse = PageResponse<PostListResponse>(emptyList(), 1, 10, 5, 1)
        every { postService.getSearchedPosts(condition) } returns pageResponse

        // when
        val result = postFacade.getSearchedPosts(condition)

        // then
        assertThat(result.totalCount).isEqualTo(5)
        verify { postService.getSearchedPosts(condition) }
    }

    @Test
    @DisplayName("savePost - 게시글을 저장하고 태그를 저장한 뒤 PostSaveResponse를 반환한다")
    fun savePost_shouldSavePostAndTagsAndReturnResponse() {
        // given
        val postRequest = PostRequest(
            categorySeq = 10L,
            title = "새 게시글",
            summary = "새 게시글 요약",
            markdownContent = "## 내용",
            status = "DRAFT",
            tags = listOf("Kotlin"),
        )
        val savedPost = createPostResponse(postSeq = 99L)

        every { transactionTemplate.execute<PostSaveResponse>(any()) } answers {
            val callback = firstArg<org.springframework.transaction.support.TransactionCallback<PostSaveResponse>>()
            callback.doInTransaction(mockk())
        }
        every { postService.savePost(postRequest, 1L) } returns savedPost
        every { postTagService.savePostTag(99L, postRequest) } just Runs

        // when
        val result = postFacade.savePost(postRequest, 1L)

        // then
        assertThat(result.postSeq).isEqualTo(99L)
        assertThat(result.title).isEqualTo("Spring Boot 시작하기")
        verify { postService.savePost(postRequest, 1L) }
        verify { postTagService.savePostTag(99L, postRequest) }
    }

    @Test
    @DisplayName("getPostEditorContents(no args) - 빈 게시글과 카테고리 목록을 반환한다")
    fun getPostEditorContents_noArgs_shouldReturnEmptyPostWithCategories() {
        // given
        val categories = listOf(PostCategory(1L, "Spring"), PostCategory(2L, "Kotlin"))
        every { categoryService.getActiveCategories() } returns categories

        // when
        val result = postFacade.getPostEditorContents()

        // then
        assertThat(result.categories).hasSize(2)
        assertThat(result.content.title).isEmpty()
        verify { categoryService.getActiveCategories() }
    }

    @Test
    @DisplayName("getPostEditorContents(postSeq) - 기존 게시글과 카테고리 목록을 반환하며 해당 카테고리가 체크된다")
    fun getPostEditorContents_withPostSeq_shouldReturnPostWithCheckedCategory() {
        // given
        val post = createPostResponse(postSeq = 5L, categorySeq = 10L)
        val categories = listOf(
            PostCategory(10L, "Spring"),
            PostCategory(20L, "Kotlin")
        )
        every { postService.getPost("5") } returns post
        every { postTagService.getTags(5L) } returns listOf("Spring")
        every { categoryService.getActiveCategories() } returns categories

        // when
        val result = postFacade.getPostEditorContents(5L)

        // then
        assertThat(result.content.postSeq).isEqualTo(5L)
        assertThat(result.categories.first { it.categorySeq == 10L }.isChecked).isTrue()
        assertThat(result.categories.first { it.categorySeq == 20L }.isChecked).isFalse()
    }

    @Test
    @DisplayName("getCategoryTree - 카테고리 트리를 반환한다")
    fun getCategoryTree_shouldReturnCategoryTree() {
        // given
        val tree = listOf(CategoryTreeResponse(
            categorySeq = 1L,
            categoryName = "Spring",
            slug = "spring",
            description = null,
            children = emptyList()
        ))
        every { categoryService.getCategoryTree() } returns tree

        // when
        val result = postFacade.getCategoryTree()

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].categoryName).isEqualTo("Spring")
        verify { categoryService.getCategoryTree() }
    }

    @Test
    @DisplayName("getCreatedSummary - OpenAI를 호출하여 요약문을 반환한다")
    fun getCreatedSummary_shouldCallOpenAiAndReturnSummary() {
        // given
        val content = "# Spring Boot\n\nSpring Boot는 자바 기반의 프레임워크입니다."
        every { openAiChatService.chat(any<AiChatRequest>()) } returns "Spring Boot는 자바 프레임워크입니다."

        // when
        val result = postFacade.getCreatedSummary(content)

        // then
        assertThat(result).isEqualTo("Spring Boot는 자바 프레임워크입니다.")
        verify { openAiChatService.chat(any<AiChatRequest>()) }
    }
}

