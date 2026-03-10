package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.dto.ExifInfo
import com.walter.lifelog.photo.dto.PhotoSearchRequest
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class PhotosQueryRepositoryTest {

    @Test
    @DisplayName("PhotoSearchRequest의 기본 페이지 값은 1이다")
    fun photoSearchRequest_defaultPage() {
        // given & when
        val request = PhotoSearchRequest()

        // then
        assertEquals(1, request.page)
        assertEquals(12, request.size)
        assertEquals(null, request.categorySeq)
    }

    @Test
    @DisplayName("PhotoSearchRequest에 카테고리를 지정할 수 있다")
    fun photoSearchRequest_withCategory() {
        // given & when
        val request = PhotoSearchRequest(categorySeq = 5L, page = 2, size = 24)

        // then
        assertEquals(5L, request.categorySeq)
        assertEquals(2, request.page)
        assertEquals(24, request.size)
    }

    @Test
    @DisplayName("PhotoSearchResponse가 올바르게 생성된다")
    fun photoSearchResponse_creation() {
        // given
        val now = LocalDateTime.now()
        val exif = ExifInfo(
            maker = "Sony",
            model = "A7IV",
            aperture = "f/2.8",
            shutter = "1/250s",
            iso = "100",
            focal = 24L,
            lens = "FE 24-70mm F2.8 GM II",
            latitude = BigDecimal("35.6762"),
            longitude = BigDecimal("139.6503")
        )

        // when
        val response = PhotoSearchResponse(
            photoSeq = 1L,
            src = "https://example.com/image.jpg",
            thumb = "https://example.com/thumb.jpg",
            title = "도쿄 타워",
            caption = "도쿄 타워에서 바라본 야경",
            categorySeq = 2L,
            categoryName = "Travel",
            tags = listOf("#Tokyo", "#Night", "#Cityscape"),
            likes = 150,
            shotAt = now,
            createdAt = now,
            exif = exif,
            userSeq = 1L,
            photographerName = "Walter"
        )

        // then
        assertEquals(1L, response.photoSeq)
        assertEquals("https://example.com/image.jpg", response.src)
        assertEquals("https://example.com/thumb.jpg", response.thumb)
        assertEquals("도쿄 타워", response.title)
        assertEquals("도쿄 타워에서 바라본 야경", response.caption)
        assertEquals(2L, response.categorySeq)
        assertEquals("Travel", response.categoryName)
        assertEquals(3, response.tags.size)
        assertEquals("#Tokyo", response.tags[0])
        assertEquals(150, response.likes)
        assertEquals(now, response.shotAt)
        assertEquals(now, response.createdAt)
        assertNotNull(response.exif)
        assertEquals("Sony", response.exif?.maker)
        assertEquals("A7IV", response.exif?.model)
        assertEquals(1L, response.userSeq)
        assertEquals("Walter", response.photographerName)
    }

    @Test
    @DisplayName("ExifInfo가 올바르게 생성된다")
    fun exifInfo_creation() {
        // given & when
        val exif = ExifInfo(
            maker = "Canon",
            model = "R5",
            aperture = "f/4",
            shutter = "1/500s",
            iso = "200",
            focal = 50L,
            lens = "RF 50mm F1.2 L USM",
            latitude = BigDecimal("37.5665"),
            longitude = BigDecimal("126.9780")
        )

        // then
        assertEquals("Canon", exif.maker)
        assertEquals("R5", exif.model)
        assertEquals("f/4", exif.aperture)
        assertEquals("1/500s", exif.shutter)
        assertEquals("200", exif.iso)
        assertEquals(50L, exif.focal)
        assertEquals("RF 50mm F1.2 L USM", exif.lens)
        assertEquals(BigDecimal("37.5665"), exif.latitude)
        assertEquals(BigDecimal("126.9780"), exif.longitude)
    }

    @Test
    @DisplayName("ExifInfo의 모든 필드가 null일 수 있다")
    fun exifInfo_allNullable() {
        // given & when
        val exif = ExifInfo(
            maker = null,
            model = null,
            aperture = null,
            shutter = null,
            iso = null,
            focal = null,
            lens = null,
            latitude = null,
            longitude = null
        )

        // then
        assertEquals(null, exif.maker)
        assertEquals(null, exif.model)
        assertEquals(null, exif.aperture)
        assertEquals(null, exif.shutter)
        assertEquals(null, exif.iso)
        assertEquals(null, exif.focal)
        assertEquals(null, exif.lens)
        assertEquals(null, exif.latitude)
        assertEquals(null, exif.longitude)
    }

    @Test
    @DisplayName("PhotoSearchResponse의 tags가 빈 리스트일 수 있다")
    fun photoSearchResponse_emptyTags() {
        // given
        val now = LocalDateTime.now()

        // when
        val response = PhotoSearchResponse(
            photoSeq = 1L,
            src = "https://example.com/image.jpg",
            thumb = null,
            title = "제목 없음",
            caption = null,
            categorySeq = null,
            categoryName = null,
            tags = emptyList(),
            likes = 0,
            shotAt = null,
            createdAt = now,
            exif = null,
            userSeq = 1L,
            photographerName = null
        )

        // then
        assertTrue(response.tags.isEmpty())
        assertEquals(null, response.thumb)
        assertEquals(null, response.caption)
        assertEquals(null, response.categorySeq)
        assertEquals(null, response.categoryName)
        assertEquals(null, response.shotAt)
        assertEquals(null, response.exif)
        assertEquals(null, response.photographerName)
    }

    @Test
    @DisplayName("PhotoSearchResponse의 태그는 List<String> 타입으로 매핑된다")
    fun photoSearchResponse_tagsAreStringList() {
        // given
        val tags = listOf("#Nature", "#Landscape", "#Mountain", "#Sunrise")

        // when
        val response = PhotoSearchResponse(
            photoSeq = 1L,
            src = "https://example.com/image.jpg",
            thumb = "https://example.com/thumb.jpg",
            title = "산 정상에서",
            caption = "일출을 기다리며",
            categorySeq = 1L,
            categoryName = "Nature",
            tags = tags,
            likes = 50,
            shotAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            exif = null,
            userSeq = 1L,
            photographerName = "Photographer"
        )

        // then
        assertEquals(4, response.tags.size)
        assertTrue(response.tags is List<String>)
        assertEquals("#Nature", response.tags[0])
        assertEquals("#Landscape", response.tags[1])
        assertEquals("#Mountain", response.tags[2])
        assertEquals("#Sunrise", response.tags[3])
    }
}
