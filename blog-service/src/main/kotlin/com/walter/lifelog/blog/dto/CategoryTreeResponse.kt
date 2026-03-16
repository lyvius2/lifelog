package com.walter.lifelog.blog.dto

import com.walter.lifelog.blog.entity.Category
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@Schema(description = "카테고리 트리 노드")
data class CategoryTreeResponse(
    @Schema(description = "카테고리 고유 식별자")
    val categorySeq: Long,

    @Schema(description = "카테고리명")
    val categoryName: String,

    @Schema(description = "URL용 슬러그")
    val slug: String,

    @Schema(description = "카테고리 설명")
    val description: String? = null,

    @Schema(description = "정렬 순서")
    val displayOrder: Int = 0,

    @Schema(description = "현재 depth (1~3)")
    val depth: Int = 1,

    @Schema(description = "하위 카테고리 목록")
    val children: List<CategoryTreeResponse> = emptyList()
) : Serializable {
    companion object {
        @JvmStatic
        fun of(category: Category, depth: Int = 1, children: List<CategoryTreeResponse> = emptyList()): CategoryTreeResponse {
            return CategoryTreeResponse(
                categorySeq = category.categorySeq!!,
                categoryName = category.categoryName,
                slug = category.slug,
                description = category.description,
                displayOrder = category.displayOrder,
                depth = depth,
                children = children
            )
        }
    }
}
