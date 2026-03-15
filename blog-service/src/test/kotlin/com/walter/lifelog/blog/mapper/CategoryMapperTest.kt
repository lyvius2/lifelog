package com.walter.lifelog.blog.mapper

import com.walter.lifelog.blog.entity.Category
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers

@DisplayName("CategoryMapper 테스트")
class CategoryMapperTest {

    private val categoryMapper: CategoryMapper = Mappers.getMapper(CategoryMapper::class.java)

    @Test
    @DisplayName("toPostInputCategory - Category Entity를 PostCategory로 정확하게 매핑한다")
    fun toPostInputCategory_shouldMapCategoryToPostCategory() {
        // given
        val category = Category(
            categorySeq = 5L,
            categoryName = "Spring Boot",
            slug = "spring-boot",
            description = "Spring Boot 관련 글",
            parentCategoryId = 1L,
            displayOrder = 3
        )

        // when
        val result = categoryMapper.toPostInputCategory(category)

        // then
        assertThat(result).isNotNull
        assertThat(result.categorySeq).isEqualTo(5L)
        assertThat(result.categoryName).isEqualTo("Spring Boot")
        assertThat(result.isChecked).isFalse()
    }

    @Test
    @DisplayName("toPostInputCategoryList - Category 리스트를 PostCategory 리스트로 정확하게 매핑한다")
    fun toPostInputCategoryList_shouldMapCategoryListToPostCategoryList() {
        // given
        val categories = listOf(
            Category(categorySeq = 1L, categoryName = "Java", slug = "java", displayOrder = 1),
            Category(categorySeq = 2L, categoryName = "Kotlin", slug = "kotlin", displayOrder = 2),
            Category(categorySeq = 3L, categoryName = "DevOps", slug = "devops", displayOrder = 3)
        )

        // when
        val result = categoryMapper.toPostInputCategoryList(categories)

        // then
        assertThat(result).hasSize(3)

        assertThat(result[0].categorySeq).isEqualTo(1L)
        assertThat(result[0].categoryName).isEqualTo("Java")
        assertThat(result[0].isChecked).isFalse()

        assertThat(result[1].categorySeq).isEqualTo(2L)
        assertThat(result[1].categoryName).isEqualTo("Kotlin")
        assertThat(result[1].isChecked).isFalse()

        assertThat(result[2].categorySeq).isEqualTo(3L)
        assertThat(result[2].categoryName).isEqualTo("DevOps")
        assertThat(result[2].isChecked).isFalse()
    }
}

