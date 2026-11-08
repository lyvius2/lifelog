package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostCategory
import com.walter.lifelog.blog.entity.Category
import com.walter.lifelog.blog.repository.CategoriesRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CategoryService 테스트")
class CategoryServiceTest {

    private val categoriesRepository: CategoriesRepository = mockk()
    private val categoryService = CategoryService(categoriesRepository)

    private fun createCategory(
        seq: Long, name: String, slug: String,
        parentId: Long? = null, displayOrder: Int = 0
    ) = Category(
        categorySeq = seq, categoryName = name, slug = slug,
        parentCategoryId = parentId, displayOrder = displayOrder
    )

    @Test
    @DisplayName("getActiveCategories - 리포지토리가 투영한 PostCategory 목록을 그대로 전달한다")
    fun getActiveCategories_shouldReturnPostCategoryList() {
        // given
        val postCategories = listOf(
            PostCategory(1L, "Java"),
            PostCategory(2L, "Kotlin")
        )
        every { categoriesRepository.findActivePostCategories() } returns postCategories

        // when
        val result = categoryService.getActiveCategories()

        // then
        assertThat(result).isEqualTo(postCategories)
        verify(exactly = 1) { categoriesRepository.findActivePostCategories() }
    }

    @Test
    @DisplayName("getActiveCategories - 활성 카테고리가 없으면 빈 리스트를 반환한다")
    fun getActiveCategories_shouldReturnEmptyListWhenNoCategories() {
        // given
        every { categoriesRepository.findActivePostCategories() } returns emptyList()

        // when
        val result = categoryService.getActiveCategories()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("getCategoryTree - 루트와 자식 카테고리를 트리 구조로 반환한다")
    fun getCategoryTree_shouldReturnTreeStructure() {
        // given
        val root1 = createCategory(1L, "Backend", "backend", parentId = null, displayOrder = 1)
        val root2 = createCategory(2L, "Frontend", "frontend", parentId = null, displayOrder = 2)
        val child1 = createCategory(3L, "Java", "java", parentId = 1L, displayOrder = 1)
        val child2 = createCategory(4L, "Kotlin", "kotlin", parentId = 1L, displayOrder = 2)
        val allCategories = listOf(root1, root2, child1, child2)

        every { categoriesRepository.findInActive() } returns allCategories

        // when
        val result = categoryService.getCategoryTree()

        // then
        assertThat(result).hasSize(2)

        val backendNode = result[0]
        assertThat(backendNode.categorySeq).isEqualTo(1L)
        assertThat(backendNode.categoryName).isEqualTo("Backend")
        assertThat(backendNode.depth).isEqualTo(1)
        assertThat(backendNode.children).hasSize(2)

        assertThat(backendNode.children[0].categorySeq).isEqualTo(3L)
        assertThat(backendNode.children[0].categoryName).isEqualTo("Java")
        assertThat(backendNode.children[0].depth).isEqualTo(2)
        assertThat(backendNode.children[0].children).isEmpty()

        assertThat(backendNode.children[1].categorySeq).isEqualTo(4L)
        assertThat(backendNode.children[1].categoryName).isEqualTo("Kotlin")
        assertThat(backendNode.children[1].depth).isEqualTo(2)

        val frontendNode = result[1]
        assertThat(frontendNode.categorySeq).isEqualTo(2L)
        assertThat(frontendNode.categoryName).isEqualTo("Frontend")
        assertThat(frontendNode.depth).isEqualTo(1)
        assertThat(frontendNode.children).isEmpty()
    }

    @Test
    @DisplayName("getCategoryTree - 3단계 depth까지만 트리를 구성하고 그 이상은 자식이 없다")
    fun getCategoryTree_shouldLimitDepthToThree() {
        // given
        val root = createCategory(1L, "Root", "root", parentId = null, displayOrder = 1)
        val depth2 = createCategory(2L, "Depth2", "depth2", parentId = 1L, displayOrder = 1)
        val depth3 = createCategory(3L, "Depth3", "depth3", parentId = 2L, displayOrder = 1)
        val depth4 = createCategory(4L, "Depth4", "depth4", parentId = 3L, displayOrder = 1)
        val allCategories = listOf(root, depth2, depth3, depth4)

        every { categoriesRepository.findInActive() } returns allCategories

        // when
        val result = categoryService.getCategoryTree()

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].depth).isEqualTo(1)
        assertThat(result[0].children).hasSize(1)
        assertThat(result[0].children[0].depth).isEqualTo(2)
        assertThat(result[0].children[0].children).hasSize(1)
        assertThat(result[0].children[0].children[0].depth).isEqualTo(3)
        // MAX_DEPTH=3이므로 depth4는 자식으로 포함되지 않는다
        assertThat(result[0].children[0].children[0].children).isEmpty()
    }

    @Test
    @DisplayName("getCategoryTree - 루트 카테고리가 없으면 빈 리스트를 반환한다")
    fun getCategoryTree_shouldReturnEmptyListWhenNoRootCategories() {
        // given
        val child = createCategory(1L, "Orphan", "orphan", parentId = 999L)
        every { categoriesRepository.findInActive() } returns listOf(child)

        // when
        val result = categoryService.getCategoryTree()

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("getCategoryTree - displayOrder에 따라 정렬된다")
    fun getCategoryTree_shouldSortByDisplayOrder() {
        // given
        val root1 = createCategory(1L, "C-Third", "c-third", parentId = null, displayOrder = 3)
        val root2 = createCategory(2L, "A-First", "a-first", parentId = null, displayOrder = 1)
        val root3 = createCategory(3L, "B-Second", "b-second", parentId = null, displayOrder = 2)
        every { categoriesRepository.findInActive() } returns listOf(root1, root2, root3)

        // when
        val result = categoryService.getCategoryTree()

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].categoryName).isEqualTo("A-First")
        assertThat(result[1].categoryName).isEqualTo("B-Second")
        assertThat(result[2].categoryName).isEqualTo("C-Third")
    }
}

