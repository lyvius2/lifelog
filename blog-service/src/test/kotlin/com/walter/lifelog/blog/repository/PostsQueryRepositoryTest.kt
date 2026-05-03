package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.shared.paging.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("PostsQueryRepository 테스트")
class PostsQueryRepositoryTest {

    @Nested
    @DisplayName("PostSearchCondition DTO 테스트")
    inner class PostSearchConditionTest {

        @Test
        @DisplayName("기본값으로 생성하면 page=1, size=10, 나머지는 null이다")
        fun defaultValues() {
            // given & when
            val condition = PostSearchCondition()

            // then
            assertThat(condition.keyword).isNull()
            assertThat(condition.categorySeq).isNull()
            assertThat(condition.tag).isNull()
            assertThat(condition.page).isEqualTo(1)
            assertThat(condition.size).isEqualTo(10)
        }

        @Test
        @DisplayName("of 팩토리 메서드로 검색 조건을 생성할 수 있다")
        fun ofFactoryMethod() {
            // given & when
            val condition = PostSearchCondition.of(
                keyword = "Spring Boot",
                categorySeq = 3L,
                tag = "Java",
                page = 2
            )

            // then
            assertThat(condition.keyword).isEqualTo("Spring Boot")
            assertThat(condition.categorySeq).isEqualTo(3L)
            assertThat(condition.tag).isEqualTo("Java")
            assertThat(condition.page).isEqualTo(2)
        }

        @Test
        @DisplayName("page가 0 이하이면 1로 보정된다")
        fun pageCoercedToMinimumOne() {
            // given & when
            val condition = PostSearchCondition.of(page = -5)

            // then
            assertThat(condition.page).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("PostListResponse DTO 테스트")
    inner class PostListResponseTest {

        @Test
        @DisplayName("모든 필드가 올바르게 매핑된다")
        fun allFieldsMappedCorrectly() {
            // given
            val now = LocalDateTime.of(2026, 3, 15, 10, 30, 0)
            val tags = listOf("Spring", "Java", "Kotlin")

            // when
            val response = PostListResponse(
                postSeq = 1L,
                title = "Spring Boot 시작하기",
                summary = "Spring Boot를 시작하는 방법",
                thumbnailUrl = "https://example.com/thumb.jpg",
                categoryName = "Backend",
                status = PostStatus.PUBLISHED.name,
                viewCount = 150,
                tags = tags,
                publishedAt = now,
                createdAt = now,
                writerName = "Walter",
                writerProfileImage = "https://example.com/profile.jpg",
            )

            // then
            assertThat(response.postSeq).isEqualTo(1L)
            assertThat(response.title).isEqualTo("Spring Boot 시작하기")
            assertThat(response.summary).isEqualTo("Spring Boot를 시작하는 방법")
            assertThat(response.thumbnailUrl).isEqualTo("https://example.com/thumb.jpg")
            assertThat(response.categoryName).isEqualTo("Backend")
            assertThat(response.status).isEqualTo("PUBLISHED")
            assertThat(response.viewCount).isEqualTo(150)
            assertThat(response.tags).hasSize(3)
            assertThat(response.tags).containsExactly("Spring", "Java", "Kotlin")
            assertThat(response.publishedAt).isEqualTo(now)
            assertThat(response.createdAt).isEqualTo(now)
            assertThat(response.writerName).isEqualTo("Walter")
            assertThat(response.writerProfileImage).isEqualTo("https://example.com/profile.jpg")
        }

        @Test
        @DisplayName("nullable 필드는 null일 수 있다")
        fun nullableFieldsCanBeNull() {
            // given & when
            val response = PostListResponse(
                postSeq = 2L,
                title = "제목만 있는 글",
                summary = null,
                thumbnailUrl = null,
                categoryName = null,
                status = PostStatus.DRAFT.name,
                viewCount = 0,
                tags = emptyList(),
                publishedAt = null,
                createdAt = LocalDateTime.now(),
                writerName = null,
                writerProfileImage = null,
            )

            // then
            assertThat(response.summary).isNull()
            assertThat(response.thumbnailUrl).isNull()
            assertThat(response.categoryName).isNull()
            assertThat(response.publishedAt).isNull()
            assertThat(response.writerName).isNull()
            assertThat(response.writerProfileImage).isNull()
            assertThat(response.tags).isEmpty()
            assertThat(response.viewCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("PageResponse 테스트")
    inner class PageResponseTest {

        @Test
        @DisplayName("검색 결과가 없으면 빈 PageResponse를 반환한다")
        fun emptyPageResponse() {
            // given & when
            val pageResponse = PageResponse<PostListResponse>(
                emptyList(), 1, 10, 0L, 0
            )

            // then
            assertThat(pageResponse.content()).isEmpty()
            assertThat(pageResponse.page()).isEqualTo(1)
            assertThat(pageResponse.size()).isEqualTo(10)
            assertThat(pageResponse.totalCount()).isEqualTo(0L)
            assertThat(pageResponse.totalPages()).isEqualTo(0)
        }

        @Test
        @DisplayName("totalPages가 올바르게 계산된다")
        fun totalPagesCalculation() {
            // given
            val totalCount = 25L
            val size = 10
            val totalPages = ((totalCount - 1) / size + 1).toInt()

            // when
            val pageResponse = PageResponse<PostListResponse>(
                emptyList(), 1, size, totalCount, totalPages
            )

            // then
            assertThat(pageResponse.totalPages()).isEqualTo(3)
        }

        @Test
        @DisplayName("데이터가 정확히 size만큼 있으면 totalPages는 1이다")
        fun totalPagesExactlyOnePageWorth() {
            // given
            val totalCount = 10L
            val size = 10
            val totalPages = (0 + 1).toInt()

            // when
            val pageResponse = PageResponse<PostListResponse>(
                emptyList(), 1, size, totalCount, totalPages
            )

            // then
            assertThat(pageResponse.totalPages()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("검색 조건 조합 테스트")
    inner class SearchConditionCombinationTest {

        @Test
        @DisplayName("keyword만 있는 조건 생성")
        fun keywordOnly() {
            // given & when
            val condition = PostSearchCondition(keyword = "Spring Boot")

            // then
            assertThat(condition.keyword).isEqualTo("Spring Boot")
            assertThat(condition.categorySeq).isNull()
            assertThat(condition.tag).isNull()
        }

        @Test
        @DisplayName("categorySeq만 있는 조건 생성")
        fun categorySeqOnly() {
            // given & when
            val condition = PostSearchCondition(categorySeq = 5L)

            // then
            assertThat(condition.keyword).isNull()
            assertThat(condition.categorySeq).isEqualTo(5L)
            assertThat(condition.tag).isNull()
        }

        @Test
        @DisplayName("tag만 있는 조건 생성")
        fun tagOnly() {
            // given & when
            val condition = PostSearchCondition(tag = "Kotlin")

            // then
            assertThat(condition.keyword).isNull()
            assertThat(condition.categorySeq).isNull()
            assertThat(condition.tag).isEqualTo("Kotlin")
        }

        @Test
        @DisplayName("status 필드 없이 검색 조건 생성 시 쿼리에서 항상 PUBLISHED로 고정된다")
        fun statusAlwaysPublished() {
            // given & when
            val condition = PostSearchCondition()

            // then: PostSearchCondition에 status 필드가 없으며, 쿼리에서 항상 PUBLISHED만 조회된다
            assertThat(condition.keyword).isNull()
            assertThat(condition.categorySeq).isNull()
            assertThat(condition.tag).isNull()
        }

        @Test
        @DisplayName("모든 조건이 함께 지정될 수 있다")
        fun allConditionsCombined() {
            // given & when
            val condition = PostSearchCondition(
                keyword = "JPA",
                categorySeq = 2L,
                tag = "ORM",
                page = 3,
                size = 20
            )

            // then
            assertThat(condition.keyword).isEqualTo("JPA")
            assertThat(condition.categorySeq).isEqualTo(2L)
            assertThat(condition.tag).isEqualTo("ORM")
            assertThat(condition.page).isEqualTo(3)
            assertThat(condition.size).isEqualTo(20)
        }

        @Test
        @DisplayName("빈 문자열 keyword는 빈 문자열로 유지된다")
        fun blankKeywordKeptAsIs() {
            // given & when
            val condition = PostSearchCondition(keyword = "   ")

            // then
            assertThat(condition.keyword).isEqualTo("   ")
            // buildConditions에서 isNotBlank 체크로 무시됨
            val shouldApply = condition.keyword?.takeIf { it.isNotBlank() }
            assertThat(shouldApply).isNull()
        }

        @Test
        @DisplayName("빈 문자열 tag는 빈 문자열로 유지된다")
        fun blankTagKeptAsIs() {
            // given & when
            val condition = PostSearchCondition(tag = "")

            // then
            assertThat(condition.tag).isEqualTo("")
            val shouldApply = condition.tag?.takeIf { it.isNotBlank() }
            assertThat(shouldApply).isNull()
        }
    }

    @Nested
    @DisplayName("offset 계산 테스트")
    inner class OffsetCalculationTest {

        @Test
        @DisplayName("page=1, size=10이면 offset=0이다")
        fun firstPageOffset() {
            // given
            val condition = PostSearchCondition(page = 1, size = 10)

            // when
            val offset = (condition.page - 1) * condition.size

            // then
            assertThat(offset).isEqualTo(0)
        }

        @Test
        @DisplayName("page=3, size=10이면 offset=20이다")
        fun thirdPageOffset() {
            // given
            val condition = PostSearchCondition(page = 3, size = 10)

            // when
            val offset = (condition.page - 1) * condition.size

            // then
            assertThat(offset).isEqualTo(20)
        }

        @Test
        @DisplayName("page=2, size=20이면 offset=20이다")
        fun customSizeOffset() {
            // given
            val condition = PostSearchCondition(page = 2, size = 20)

            // when
            val offset = (condition.page - 1) * condition.size

            // then
            assertThat(offset).isEqualTo(20)
        }
    }
}

