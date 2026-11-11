package com.walter.lifelog.content.service

import com.walter.lifelog.content.entity.StaticPage
import com.walter.lifelog.content.entity.code.StaticPageSourceType
import com.walter.lifelog.content.repository.StaticPageRepository
import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.dao.DuplicateKeyException

@DisplayName("StaticPageService 테스트")
class StaticPageServiceTest {

    private val staticPageRepository: StaticPageRepository = mockk()
    private val cache: Cache = mockk(relaxed = true)
    private val cacheManager: CacheManager = mockk { every { getCache(any()) } returns cache }
    private val dynamicCacheRegistry: DynamicCacheRegistry = mockk(relaxed = true)
    private val sut = StaticPageService(
        staticPageRepository = staticPageRepository,
        cacheManager = cacheManager,
        dynamicCacheRegistry = dynamicCacheRegistry,
    )

    @Test
    @DisplayName("getHtml - 활성 상태의 경로면 저장된 마크업을 돌려준다")
    fun getHtml_activePath_returnsMarkup() {
        // given
        val staticPage = createStaticPage(path = "my-page", html = "<html><body>hi</body></html>")
        every { staticPageRepository.findByPathAndIsActiveTrue("my-page") } returns staticPage

        // when
        val result = sut.getHtml("my-page")

        // then
        assertThat(result).isEqualTo("<html><body>hi</body></html>")
    }

    @Test
    @DisplayName("getHtml - 등록되지 않은 경로면 null 을 돌려준다")
    fun getHtml_unknownPath_returnsNull() {
        // given
        every { staticPageRepository.findByPathAndIsActiveTrue("nope") } returns null

        // when
        val result = sut.getHtml("nope")

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("resolvePath - 규칙에 맞고 쓰이지 않은 경로면 그대로 쓴다")
    fun resolvePath_validAndUnused_returnsAsIs() {
        // given
        every { staticPageRepository.existsByPath("My_Page-1") } returns false

        // when
        val result = sut.resolvePath("My_Page-1")

        // then
        assertThat(result).isEqualTo("My_Page-1")
    }

    @Test
    @DisplayName("resolvePath - 이미 쓰이는 경로면 중복으로 거부한다")
    fun resolvePath_duplicated_throws() {
        // given
        every { staticPageRepository.existsByPath("taken") } returns true

        // when & then
        assertThatThrownBy { sut.resolvePath("taken") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 사용 중인 경로입니다")
    }

    @Test
    @DisplayName("resolvePath - 허용하지 않는 문자가 있으면 거부한다")
    fun resolvePath_invalidCharacters_throws() {
        // given
        val invalidPaths = listOf("한글경로", "with space", "dot.path", "slash/path", "plus+path")

        // when & then
        invalidPaths.forEach { path ->
            assertThatThrownBy { sut.resolvePath(path) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("영문·숫자·하이픈·언더바")
        }
    }

    @Test
    @DisplayName("resolvePath - 20자를 넘으면 거부하고 20자는 허용한다")
    fun resolvePath_lengthBoundary_rejectsOverTwenty() {
        // given
        val exactly20 = "a".repeat(20)
        val over20 = "a".repeat(21)
        every { staticPageRepository.existsByPath(exactly20) } returns false

        // when & then
        assertThat(sut.resolvePath(exactly20)).isEqualTo(exactly20)
        assertThatThrownBy { sut.resolvePath(over20) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("resolvePath - 경로를 비워 두면 소문자와 숫자 8자로 자동 생성한다")
    fun resolvePath_blank_generatesEightCharPath() {
        // given
        every { staticPageRepository.existsByPath(any()) } returns false

        // when
        val result = sut.resolvePath(null)
        val resultFromBlank = sut.resolvePath("   ")

        // then
        assertThat(result).hasSize(8).matches("^[a-z0-9]{8}$")
        assertThat(resultFromBlank).hasSize(8).matches("^[a-z0-9]{8}$")
    }

    @Test
    @DisplayName("resolvePath - 자동 생성한 경로가 겹치면 다시 뽑는다")
    fun resolvePath_generatedCollision_retries() {
        // given
        every { staticPageRepository.existsByPath(any()) } returnsMany listOf(true, true, false)

        // when
        val result = sut.resolvePath(null)

        // then
        assertThat(result).matches("^[a-z0-9]{8}$")
        verify(exactly = 3) { staticPageRepository.existsByPath(any()) }
    }

    @Test
    @DisplayName("register - 마크업과 경로를 저장하고 해당 키의 캐시를 비운다")
    fun register_savesAndEvictsCache() {
        // given
        val markup = "<html><body>테스트</body></html>"
        val saved = slot<StaticPage>()
        every { staticPageRepository.existsByPath("my-page") } returns false
        every { staticPageRepository.save(capture(saved)) } answers { saved.captured }

        // when
        val result = sut.register(
            requestedPath = "my-page",
            sourceType = StaticPageSourceType.FILE,
            markup = markup,
            title = "테스트 문서",
            sourceUrl = null,
            sourceFileName = "page.html",
            userSeq = 1L,
        )

        // then
        assertThat(result.path).isEqualTo("my-page")
        assertThat(result.url).isEqualTo("/static/my-page")
        assertThat(result.byteSize).isEqualTo(markup.toByteArray(Charsets.UTF_8).size)
        assertThat(saved.captured.html).isEqualTo(markup)
        assertThat(saved.captured.isActive).isTrue()
        verify(exactly = 1) { cache.evict("my-page") }
    }

    @Test
    @DisplayName("delete - 물리 삭제하지 않고 isActive 를 false 로 바꾸고 캐시를 비운다")
    fun delete_softDeletesAndEvictsCache() {
        // given
        val staticPage = createStaticPage(path = "my-page")
        val saved = slot<StaticPage>()
        every { staticPageRepository.findByPathAndIsActiveTrue("my-page") } returns staticPage
        every { staticPageRepository.save(capture(saved)) } answers { saved.captured }

        // when
        sut.delete("my-page")

        // then
        assertThat(saved.captured.isActive).isFalse()
        verify(exactly = 1) { cache.evict("my-page") }
        verify(exactly = 0) { staticPageRepository.delete(any()) }
    }

    @Test
    @DisplayName("delete - 등록되지 않은 경로면 거부한다")
    fun delete_unknownPath_throws() {
        // given
        every { staticPageRepository.findByPathAndIsActiveTrue("nope") } returns null

        // when & then
        assertThatThrownBy { sut.delete("nope") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("등록되지 않은 경로입니다")
    }

    @Test
    @DisplayName("register - unique index 에서 중복이 걸리면 중복 메시지로 바꿔 돌려준다")
    fun register_duplicateKeyFromIndex_throwsIllegalArgument() {
        // given
        every { staticPageRepository.existsByPath("my-page") } returns false
        every { staticPageRepository.save(any()) } throws DuplicateKeyException("E11000 duplicate key")

        // when & then
        assertThatThrownBy {
            sut.register(
                requestedPath = "my-page",
                sourceType = StaticPageSourceType.FILE,
                markup = "<html><body>테스트 본문입니다</body></html>",
                title = null,
                sourceUrl = null,
                sourceFileName = "page.html",
                userSeq = 1L,
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 사용 중인 경로입니다")
    }

    private fun createStaticPage(
        path: String = "my-page",
        html: String = "<html><body>content</body></html>",
    ) = StaticPage(
        id = "507f1f77bcf86cd799439011",
        path = path,
        sourceType = StaticPageSourceType.FILE,
        sourceFileName = "page.html",
        title = "제목",
        html = html,
        byteSize = html.toByteArray(Charsets.UTF_8).size,
        userSeq = 1L,
    )
}
