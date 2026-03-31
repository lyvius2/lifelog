package com.walter.lifelog.content.service

import com.walter.lifelog.content.dto.ContentDocumentRequest
import com.walter.lifelog.content.entity.ContentDocuments
import com.walter.lifelog.content.entity.code.ContentType
import com.walter.lifelog.content.repository.ContentDocumentRepository
import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.time.LocalDateTime

@DisplayName("ContentService 테스트")
class ContentServiceTest {

    private val contentDocumentRepository: ContentDocumentRepository = mockk()
    private val cacheManager: CacheManager = mockk()
    private val dynamicCacheRegistry: DynamicCacheRegistry = mockk(relaxed = true)
    private val contentService = ContentService(contentDocumentRepository, cacheManager, dynamicCacheRegistry)

    private fun createDocument(
        id: String = "doc1",
        contentType: ContentType = ContentType.PROFILE,
        content: HashMap<String, Any> = hashMapOf("title" to "테스트"),
        updatedAt: LocalDateTime = LocalDateTime.of(2026, 3, 31, 12, 0)
    ) = ContentDocuments(
        id = id,
        contentType = contentType,
        content = content,
        updatedAt = updatedAt,
    )

    @Test
    @DisplayName("getContentByType - 콘텐츠가 존재하면 content를 반환한다")
    fun getContentByType_shouldReturnContentWhenDocumentExists() {
        // given
        val content = hashMapOf<String, Any>("title" to "안녕하세요")
        val document = createDocument(content = content)
        every { contentDocumentRepository.findByContentType(ContentType.PROFILE) } returns document

        // when
        val result = contentService.getContentByType(ContentType.PROFILE)

        // then
        assertThat(result).isNotEmpty
        assertThat(result["title"]).isEqualTo("안녕하세요")
        verify(exactly = 1) { contentDocumentRepository.findByContentType(ContentType.PROFILE) }
    }

    @Test
    @DisplayName("getContentByType - 콘텐츠가 존재하지 않으면 빈 HashMap을 반환한다")
    fun getContentByType_shouldReturnEmptyMapWhenDocumentNotFound() {
        // given
        every { contentDocumentRepository.findByContentType(ContentType.INTRO) } returns null

        // when
        val result = contentService.getContentByType(ContentType.INTRO)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("getAllTypeInfos - 모든 ContentType 정보를 반환하고 문서 존재 여부를 표시한다")
    fun getAllTypeInfos_shouldReturnAllTypesWithDocumentExistence() {
        // given
        val existingDoc = createDocument(contentType = ContentType.PROFILE)
        every { contentDocumentRepository.findAll() } returns listOf(existingDoc)

        // when
        val result = contentService.getAllTypeInfos()

        // then
        assertThat(result).hasSize(ContentType.entries.size)
        val profileInfo = result.find { it.contentType == ContentType.PROFILE.name }
        assertThat(profileInfo).isNotNull
        assertThat(profileInfo!!.hasDocument).isTrue()

        val introInfo = result.find { it.contentType == ContentType.INTRO.name }
        assertThat(introInfo).isNotNull
        assertThat(introInfo!!.hasDocument).isFalse()
    }

    @Test
    @DisplayName("getDocumentByType - 문서가 존재하면 해당 문서 응답을 반환한다")
    fun getDocumentByType_shouldReturnExistingDocument() {
        // given
        val content = hashMapOf<String, Any>("name" to "Walter")
        val document = createDocument(contentType = ContentType.PROFILE, content = content)
        every { contentDocumentRepository.findByContentType(ContentType.PROFILE) } returns document

        // when
        val result = contentService.getDocumentByType(ContentType.PROFILE)

        // then
        assertThat(result.contentType).isEqualTo("PROFILE")
        assertThat(result.typeDescription).isEqualTo("개발자 자기소개")
        assertThat(result.content["name"]).isEqualTo("Walter")
        assertThat(result.id).isEqualTo("doc1")
    }

    @Test
    @DisplayName("getDocumentByType - 문서가 없으면 빈 content의 응답을 반환한다")
    fun getDocumentByType_shouldReturnEmptyDocumentWhenNotFound() {
        // given
        every { contentDocumentRepository.findByContentType(ContentType.ARCHITECTURE) } returns null

        // when
        val result = contentService.getDocumentByType(ContentType.ARCHITECTURE)

        // then
        assertThat(result.contentType).isEqualTo("ARCHITECTURE")
        assertThat(result.content).isEmpty()
        assertThat(result.id).isNull()
    }

    @Test
    @DisplayName("upsertDocument - 기존 문서가 없으면 새로 생성한다")
    fun upsertDocument_shouldCreateNewDocumentWhenNotExists() {
        // given
        val request = ContentDocumentRequest(
            contentType = "INTRO",
            content = hashMapOf("title" to "소개 페이지")
        )
        val savedDocument = createDocument(id = "newDoc", contentType = ContentType.INTRO, content = request.content)
        val mockCache: Cache = mockk(relaxed = true)

        every { contentDocumentRepository.findByContentType(ContentType.INTRO) } returns null
        every { contentDocumentRepository.save(any()) } returns savedDocument
        every { cacheManager.getCache("contentByType") } returns mockCache

        // when
        val result = contentService.upsertDocument(request)

        // then
        assertThat(result.contentType).isEqualTo("INTRO")
        assertThat(result.content["title"]).isEqualTo("소개 페이지")
        verify(exactly = 1) { contentDocumentRepository.save(any()) }
        verify(exactly = 1) { mockCache.evict("INTRO") }
    }

    @Test
    @DisplayName("upsertDocument - 기존 문서가 있으면 content를 업데이트한다")
    fun upsertDocument_shouldUpdateExistingDocument() {
        // given
        val existingDoc = createDocument(contentType = ContentType.PROFILE, content = hashMapOf("title" to "기존"))
        val request = ContentDocumentRequest(
            contentType = "PROFILE",
            content = hashMapOf("title" to "수정됨")
        )
        val updatedDoc = existingDoc.copy(content = request.content)
        val mockCache: Cache = mockk(relaxed = true)

        every { contentDocumentRepository.findByContentType(ContentType.PROFILE) } returns existingDoc
        every { contentDocumentRepository.save(any()) } returns updatedDoc
        every { cacheManager.getCache("contentByType") } returns mockCache

        // when
        val result = contentService.upsertDocument(request)

        // then
        assertThat(result.contentType).isEqualTo("PROFILE")
        assertThat(result.content["title"]).isEqualTo("수정됨")
        verify(exactly = 1) { contentDocumentRepository.save(match { it.id == existingDoc.id }) }
        verify(exactly = 1) { mockCache.evict("PROFILE") }
    }
}

