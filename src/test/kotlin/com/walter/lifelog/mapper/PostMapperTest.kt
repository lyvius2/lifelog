package com.walter.lifelog.mapper

import com.walter.lifelog.dto.PostResponse
import com.walter.lifelog.entity.Post
import com.walter.lifelog.entity.code.PostStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers
import java.time.LocalDateTime

@DisplayName("PostMapper 테스트")
class PostMapperTest {

    private val postMapper: PostMapper = Mappers.getMapper(PostMapper::class.java)

    @Test
    @DisplayName("toDto - Post Entity를 PostResponse로 정확하게 매핑한다")
    fun toDto_shouldMapPostToPostResponse() {
        // given
        val now = LocalDateTime.now()
        val post = Post(
            postSeq = 1L,
            userSeq = 100L,
            categorySeq = 10L,
            title = "테스트 게시글",
            slug = "test-post",
            summary = "테스트 요약",
            content = "테스트 내용입니다.",
            markdownContent = "# 테스트 내용입니다.",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            status = PostStatus.PUBLISHED,
            viewCount = 50,
            isFeatured = true,
            writerUserSeq = 100L,
            publishedAt = now.minusDays(1),
            createdAt = now.minusDays(2),
            updatedAt = now
        )

        // when
        val response: PostResponse = postMapper.toDto(post)

        // then
        assertThat(response).isNotNull
        assertThat(response.postSeq).isEqualTo(post.postSeq)
        assertThat(response.userSeq).isEqualTo(post.userSeq)
        assertThat(response.title).isEqualTo(post.title)
        assertThat(response.summary).isEqualTo(post.summary)
        assertThat(response.content).isEqualTo(post.content)
        assertThat(response.thumbnailUrl).isEqualTo(post.thumbnailUrl)
        assertThat(response.viewCount).isEqualTo(post.viewCount)
        assertThat(response.isFeatured).isEqualTo(post.isFeatured)
        assertThat(response.publishedAt).isEqualTo(post.publishedAt)
        assertThat(response.createdAt).isEqualTo(post.createdAt)
        assertThat(response.updatedAt).isEqualTo(post.updatedAt)
    }

    @Test
    @DisplayName("toDtoList - Post Entity 리스트를 PostResponse 리스트로 정확하게 매핑한다")
    fun toDtoList_shouldMapPostListToPostResponseList() {
        // given
        val now = LocalDateTime.now()
        val post1 = Post(
            postSeq = 1L,
            userSeq = 100L,
            categorySeq = 10L,
            title = "첫 번째 게시글",
            slug = "first-post",
            summary = "첫 번째 요약",
            content = "첫 번째 내용",
            markdownContent = "# 첫 번째 내용",
            thumbnailUrl = "https://example.com/thumbnail1.jpg",
            status = PostStatus.PUBLISHED,
            viewCount = 100,
            isFeatured = true,
            writerUserSeq = 100L,
            publishedAt = now.minusDays(1),
            createdAt = now.minusDays(2),
            updatedAt = now
        )

        val post2 = Post(
            postSeq = 2L,
            userSeq = 101L,
            categorySeq = 11L,
            title = "두 번째 게시글",
            slug = "second-post",
            summary = "두 번째 요약",
            content = "두 번째 내용",
            markdownContent = "# 두 번째 내용",
            thumbnailUrl = "https://example.com/thumbnail2.jpg",
            status = PostStatus.DRAFT,
            viewCount = 0,
            isFeatured = false,
            writerUserSeq = 101L,
            publishedAt = null,
            createdAt = now.minusDays(1),
            updatedAt = now
        )

        val posts = listOf(post1, post2)

        // when
        val responses: List<PostResponse> = postMapper.toDtoList(posts)

        // then
        assertThat(responses).isNotNull
        assertThat(responses).hasSize(2)

        // 첫 번째 게시글 검증
        val response1 = responses[0]
        assertThat(response1.postSeq).isEqualTo(post1.postSeq)
        assertThat(response1.userSeq).isEqualTo(post1.userSeq)
        assertThat(response1.title).isEqualTo(post1.title)
        assertThat(response1.summary).isEqualTo(post1.summary)
        assertThat(response1.content).isEqualTo(post1.content)
        assertThat(response1.thumbnailUrl).isEqualTo(post1.thumbnailUrl)
        assertThat(response1.viewCount).isEqualTo(post1.viewCount)
        assertThat(response1.isFeatured).isEqualTo(post1.isFeatured)
        assertThat(response1.publishedAt).isEqualTo(post1.publishedAt)
        assertThat(response1.createdAt).isEqualTo(post1.createdAt)
        assertThat(response1.updatedAt).isEqualTo(post1.updatedAt)

        // 두 번째 게시글 검증
        val response2 = responses[1]
        assertThat(response2.postSeq).isEqualTo(post2.postSeq)
        assertThat(response2.userSeq).isEqualTo(post2.userSeq)
        assertThat(response2.title).isEqualTo(post2.title)
        assertThat(response2.summary).isEqualTo(post2.summary)
        assertThat(response2.content).isEqualTo(post2.content)
        assertThat(response2.thumbnailUrl).isEqualTo(post2.thumbnailUrl)
        assertThat(response2.viewCount).isEqualTo(post2.viewCount)
        assertThat(response2.isFeatured).isEqualTo(post2.isFeatured)
        assertThat(response2.publishedAt).isEqualTo(post2.publishedAt)
        assertThat(response2.createdAt).isEqualTo(post2.createdAt)
        assertThat(response2.updatedAt).isEqualTo(post2.updatedAt)
    }
}

