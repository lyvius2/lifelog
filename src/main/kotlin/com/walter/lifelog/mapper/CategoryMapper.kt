package com.walter.lifelog.mapper

import com.walter.lifelog.controller.dto.PostCategory
import com.walter.lifelog.entity.Category
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface CategoryMapper {
    @Mapping(target = "isChecked", constant = "false")
    fun toPostInputCategory(category: Category): PostCategory

    @Mapping(target = "isChecked", constant = "false")
    fun toPostInputCategoryList(categories: List<Category>): List<PostCategory>
}

