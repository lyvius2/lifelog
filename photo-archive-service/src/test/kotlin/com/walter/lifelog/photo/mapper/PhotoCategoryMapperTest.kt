package com.walter.lifelog.photo.mapper

import com.walter.lifelog.photo.entity.PhotoCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers

@DisplayName("PhotoCategoryMapper 테스트")
class PhotoCategoryMapperTest {

    private val mapper: PhotoCategoryMapper = Mappers.getMapper(PhotoCategoryMapper::class.java)

    @Test
    @DisplayName("toResponse - PhotoCategory Entity를 PhotoCategoryResponse로 정확하게 매핑한다")
    fun toResponse_shouldMapEntityToResponse() {
        // given
        val photoCategory = PhotoCategory(
            categorySeq = 1L,
            categoryName = "My Car",
            icon = "🚗"
        )

        // when
        val result = mapper.toResponse(photoCategory)

        // then
        assertThat(result.categorySeq).isEqualTo(1L)
        assertThat(result.name).isEqualTo("My Car")
        assertThat(result.icon).isEqualTo("🚗")
    }

    @Test
    @DisplayName("toResponseList - PhotoCategory 리스트를 PhotoCategoryResponse 리스트로 정확하게 매핑한다")
    fun toResponseList_shouldMapEntityListToResponseList() {
        // given
        val categories = listOf(
            PhotoCategory(categorySeq = 1L, categoryName = "My Car", icon = "🚗"),
            PhotoCategory(categorySeq = 2L, categoryName = "Travel", icon = "✈️"),
            PhotoCategory(categorySeq = 3L, categoryName = "Nature", icon = "🌿")
        )

        // when
        val result = mapper.toResponseList(categories)

        // then
        assertThat(result).hasSize(3)

        assertThat(result[0].categorySeq).isEqualTo(1L)
        assertThat(result[0].name).isEqualTo("My Car")
        assertThat(result[0].icon).isEqualTo("🚗")

        assertThat(result[1].categorySeq).isEqualTo(2L)
        assertThat(result[1].name).isEqualTo("Travel")
        assertThat(result[1].icon).isEqualTo("✈️")

        assertThat(result[2].categorySeq).isEqualTo(3L)
        assertThat(result[2].name).isEqualTo("Nature")
        assertThat(result[2].icon).isEqualTo("🌿")
    }
}

