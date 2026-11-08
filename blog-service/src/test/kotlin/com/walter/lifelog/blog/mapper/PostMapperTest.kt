package com.walter.lifelog.blog.mapper

import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.blog.entity.code.PostStatus
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
        assertThat(response.publishedAt).isEqualTo(post.publishedAt)
        assertThat(response.createdAt).isEqualTo(post.createdAt)
        assertThat(response.updatedAt).isEqualTo(post.updatedAt)
    }

}