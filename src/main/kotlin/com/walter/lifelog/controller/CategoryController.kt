package com.walter.lifelog.controller

import com.walter.lifelog.controller.dto.CategoryTreeResponse
import com.walter.lifelog.controller.dto.Rest
import com.walter.lifelog.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "카테고리", description = "카테고리 관리 API")
@RequestMapping("/api/category")
@RestController
class CategoryController(
    private val categoryService: CategoryService,
) {
    @Operation(
        summary = "카테고리 트리 조회",
        description = "활성화된 카테고리를 최대 3 depth 트리 구조로 조회합니다."
    )
    @GetMapping("/tree")
    fun getCategoryTree(): Rest<List<CategoryTreeResponse>> {
        return Rest.ok(categoryService.getCategoryTree())
    }
}
