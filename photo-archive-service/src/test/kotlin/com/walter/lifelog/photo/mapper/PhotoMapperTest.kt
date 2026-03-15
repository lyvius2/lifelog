package com.walter.lifelog.photo.mapper

import com.walter.lifelog.photo.dto.UploadRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("PhotoMapper 테스트")
class PhotoMapperTest {

    private val mapper: PhotoMapper = Mappers.getMapper(PhotoMapper::class.java)

    @Test
    @DisplayName("toEntity - UploadRequest를 Photo Entity로 정확하게 매핑한다")
    fun toEntity_shouldMapUploadRequestToPhotoEntity() {
        // given
        val shotAt = LocalDateTime.of(2026, 3, 15, 14, 30, 0)
        val uploadRequest = UploadRequest(
            title = "벚꽃이 핀 거리",
            caption = "봄날의 풍경",
            categorySeq = 2L,
            tags = listOf("#봄", "#벚꽃"),
            maker = "SONY",
            model = "ILCE-7M4",
            lens = "FE 24-70mm F2.8 GM II",
            aperture = "f/2.8",
            shutter = "1/250s",
            iso = "400",
            focalLength = 50L,
            flash = "Off",
            latitude = 37.56653,
            longitude = 126.97797,
            shotAt = shotAt
        )
        val mainFileName = "cherry_blossom.jpg"
        val subFileName = "cherry_blossom_thumb.jpg"
        val userSeq = 1L
        val folderPath = "photo/lifelog"

        // when
        val photo = mapper.toEntity(uploadRequest, mainFileName, subFileName, userSeq, folderPath)

        // then
        assertThat(photo.photoSeq).isNull()
        assertThat(photo.userSeq).isEqualTo(1L)
        assertThat(photo.title).isEqualTo("벚꽃이 핀 거리")
        assertThat(photo.caption).isEqualTo("봄날의 풍경")
        assertThat(photo.categorySeq).isEqualTo(2L)
        assertThat(photo.imageUrl).isEqualTo("/photo/lifelog/cherry_blossom.jpg")
        assertThat(photo.thumbnailUrl).isEqualTo("/photo/lifelog/thumb/cherry_blossom_thumb.jpg")
        assertThat(photo.exifMaker).isEqualTo("SONY")
        assertThat(photo.exifModel).isEqualTo("ILCE-7M4")
        assertThat(photo.exifLens).isEqualTo("FE 24-70mm F2.8 GM II")
        assertThat(photo.exifAperture).isEqualTo("f/2.8")
        assertThat(photo.exifShutter).isEqualTo("1/250s")
        assertThat(photo.exifIso).isEqualTo("400")
        assertThat(photo.exifFocalLength).isEqualTo(50L)
        assertThat(photo.exifFlash).isEqualTo("Off")
        assertThat(photo.gpsLatitude).isEqualByComparingTo(BigDecimal.valueOf(37.56653))
        assertThat(photo.gpsLongitude).isEqualByComparingTo(BigDecimal.valueOf(126.97797))
        assertThat(photo.shotAt).isEqualTo(shotAt)
        assertThat(photo.likeCount).isEqualTo(0)
        assertThat(photo.isActive).isTrue()
    }

    @Test
    @DisplayName("toEntity - EXIF와 GPS가 null인 경우에도 정상 매핑한다")
    fun toEntity_shouldHandleNullExifAndGps() {
        // given
        val uploadRequest = UploadRequest(
            title = "제목만 있는 사진",
            caption = "설명 없음",
            categorySeq = 1L,
            tags = null,
            maker = null,
            model = null,
            lens = null,
            aperture = null,
            shutter = null,
            iso = null,
            focalLength = null,
            flash = null,
            latitude = null,
            longitude = null,
            shotAt = null
        )

        // when
        val photo = mapper.toEntity(uploadRequest, "photo.jpg", "photo_thumb.jpg", 5L, "photo/test")

        // then
        assertThat(photo.userSeq).isEqualTo(5L)
        assertThat(photo.title).isEqualTo("제목만 있는 사진")
        assertThat(photo.imageUrl).isEqualTo("/photo/test/photo.jpg")
        assertThat(photo.thumbnailUrl).isEqualTo("/photo/test/thumb/photo_thumb.jpg")
        assertThat(photo.exifMaker).isNull()
        assertThat(photo.exifModel).isNull()
        assertThat(photo.exifLens).isNull()
        assertThat(photo.exifAperture).isNull()
        assertThat(photo.exifShutter).isNull()
        assertThat(photo.exifIso).isNull()
        assertThat(photo.exifFocalLength).isNull()
        assertThat(photo.exifFlash).isNull()
        assertThat(photo.gpsLatitude).isNull()
        assertThat(photo.gpsLongitude).isNull()
        assertThat(photo.shotAt).isNull()
    }

    @Test
    @DisplayName("doubleToBigDecimal - Double 값을 BigDecimal로 변환한다")
    fun doubleToBigDecimal_shouldConvertDoubleToBigDecimal() {
        // when
        val result = mapper.doubleToBigDecimal(37.56653)

        // then
        assertThat(result).isNotNull
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(37.56653))
    }

    @Test
    @DisplayName("doubleToBigDecimal - null 입력 시 null을 반환한다")
    fun doubleToBigDecimal_shouldReturnNullForNullInput() {
        // when
        val result = mapper.doubleToBigDecimal(null)

        // then
        assertThat(result).isNull()
    }
}

