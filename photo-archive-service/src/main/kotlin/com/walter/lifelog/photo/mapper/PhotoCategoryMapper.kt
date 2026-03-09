package com.walter.lifelog.photo.mapper

import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.entity.PhotoCategory
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PhotoCategoryMapper {
    @Mapping(target = "name", source = "categoryName")
    @Mapping(target = "icon", source = "icon")
    fun toResponse(photoCategory: PhotoCategory): PhotoCategoryResponse

    fun toResponseList(photoCategories: List<PhotoCategory>): List<PhotoCategoryResponse>
}