package com.walter.lifelog.mapper

import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.entity.Post
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PostMapper {
    @Mapping(target = "isFeatured", source = "featured")
    fun toDto(post: Post): PostResponse

    @Mapping(target = "isFeatured", source = "featured")
    fun toDtoList(posts: List<Post>): List<PostResponse>
}

