package com.walter.lifelog.content.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.RedisSerializer

@DisplayName("StaticPageContent 테스트")
class StaticPageContentTest {

    /*
      live 는 이 값을 Redis 에 JSON 으로 넣습니다. 직렬화기는 RedisCacheConfig 의
      RedisSerializer.json() 이고 Kotlin 모듈이 붙어 있지 않으므로, 이름이나
      생성자가 어긋나면 되읽을 때 깨집니다. 같은 직렬화기로 왕복을 확인합니다.
    */
    private val serializer = RedisSerializer.json()

    @Test
    @DisplayName("Redis 직렬화기로 왕복해도 마크업과 공개 여부가 그대로 남는다")
    fun redisSerializer_roundTrip_keepsValues() {
        // given
        val content = StaticPageContent(html = "<html><body>본문</body></html>", isPublic = false)

        // when
        val restored = serializer.deserialize(serializer.serialize(content)) as StaticPageContent

        // then
        assertThat(restored.html).isEqualTo("<html><body>본문</body></html>")
        assertThat(restored.isPublic).isFalse()
    }

    @Test
    @DisplayName("공개 페이지도 왕복 후 isPublic 이 true 로 남는다")
    fun redisSerializer_roundTripPublic_keepsTrue() {
        // given
        val content = StaticPageContent(html = "<html><body>hi</body></html>", isPublic = true)

        // when
        val restored = serializer.deserialize(serializer.serialize(content)) as StaticPageContent

        // then
        assertThat(restored.isPublic).isTrue()
    }

    @Test
    @DisplayName("JSON 필드명은 public 이 아니라 isPublic 으로 나간다")
    fun jsonFieldName_isPublic_notPublic() {
        // given
        val content = StaticPageContent(html = "<p>x</p>", isPublic = true)

        // when
        val json = String(serializer.serialize(content)!!)

        // then
        assertThat(json).contains("\"isPublic\"")
        assertThat(json).doesNotContain("\"public\"")
    }
}
