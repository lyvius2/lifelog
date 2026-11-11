package com.walter.lifelog.content.service

import com.walter.lifelog.content.dto.StaticPageResponse
import com.walter.lifelog.content.entity.StaticPage
import com.walter.lifelog.content.entity.code.StaticPageSourceType
import com.walter.lifelog.content.repository.StaticPageRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry
import org.springframework.cache.CacheManager
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class StaticPageService(
    private val staticPageRepository: StaticPageRepository,
    private val cacheManager: CacheManager,
    private val dynamicCacheRegistry: DynamicCacheRegistry,
) {
    companion object {
        private const val CACHE_NAME = "staticPageHtml"
        private const val CACHE_TTL_MINUTES = 60L
        private const val PATH_MAX_LENGTH = 20
        private const val GENERATED_PATH_LENGTH = 8
        private const val GENERATED_PATH_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
        private const val GENERATE_ATTEMPTS = 10
        private val PATH_PATTERN = Regex("^[A-Za-z0-9_-]{1,$PATH_MAX_LENGTH}$")
    }

    private val random = SecureRandom()

    /**
     * /static/{path} 가 서비스할 마크업. 없으면 null 입니다.
     *
     * Cache aside — 캐시를 먼저 보고 없을 때만 MongoDB 를 읽습니다. 인터셉터가 null 은
     * 캐싱하지 않으므로 없는 경로가 캐시를 채우지 않습니다.
     */
    @DynamicCacheable(value = [CACHE_NAME], key = "#path", ttlMinutes = CACHE_TTL_MINUTES)
    fun getHtml(path: String): String? {
        return staticPageRepository.findByPathAndIsActiveTrue(path)?.html
    }

    fun register(
        requestedPath: String?,
        sourceType: StaticPageSourceType,
        markup: String,
        title: String?,
        sourceUrl: String?,
        sourceFileName: String?,
        userSeq: Long,
    ): StaticPageResponse {
        val path = resolvePath(requestedPath)
        val staticPage = StaticPage(
            path = path,
            sourceType = sourceType,
            sourceUrl = sourceUrl,
            sourceFileName = sourceFileName,
            title = title,
            html = markup,
            byteSize = markup.toByteArray(Charsets.UTF_8).size,
            userSeq = userSeq,
        )
        // resolvePath 의 중복 확인과 save 사이는 원자적이지 않습니다. 최종 판정은
        // path 의 unique index 이고, 거기서 걸리면 중복 메시지로 바꿔 돌려줍니다.
        val saved = try {
            staticPageRepository.save(staticPage)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException("이미 사용 중인 경로입니다: $path", e)
        }
        evictHtmlCache(path)
        return StaticPageResponse.of(saved)
    }

    fun getPages(): List<StaticPageResponse> =
        staticPageRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().map { StaticPageResponse.of(it) }

    fun delete(path: String) {
        val staticPage = staticPageRepository.findByPathAndIsActiveTrue(path)
        requireNotNull(staticPage) { "등록되지 않은 경로입니다: $path" }
        staticPageRepository.save(staticPage.copy(isActive = false))
        evictHtmlCache(path)
    }

    /** 입력이 있으면 규칙과 중복만 확인하고, 없으면 쓰이지 않은 경로를 만들어 돌려줍니다. */
    fun resolvePath(requestedPath: String?): String {
        val requested = requestedPath?.trim()
        if (requested.isNullOrBlank()) return generatePath()

        require(PATH_PATTERN.matches(requested)) {
            "경로는 영문·숫자·하이픈·언더바 조합으로 최대 ${PATH_MAX_LENGTH}자까지 쓸 수 있습니다."
        }
        require(!staticPageRepository.existsByPath(requested)) { "이미 사용 중인 경로입니다: $requested" }
        return requested
    }

    private fun generatePath(): String {
        repeat(GENERATE_ATTEMPTS) {
            val candidate = (1..GENERATED_PATH_LENGTH)
                .map { GENERATED_PATH_ALPHABET[random.nextInt(GENERATED_PATH_ALPHABET.length)] }
                .joinToString("")
            if (!staticPageRepository.existsByPath(candidate)) return candidate
        }
        throw IllegalStateException("경로를 자동 생성하지 못했습니다. 경로를 직접 지정해주세요.")
    }

    private fun evictHtmlCache(path: String) {
        dynamicCacheRegistry.register(CACHE_NAME, CACHE_TTL_MINUTES)
        cacheManager.getCache(CACHE_NAME)?.evict(path)
    }
}
