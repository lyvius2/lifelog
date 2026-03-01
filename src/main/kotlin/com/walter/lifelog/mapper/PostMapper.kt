package com.walter.lifelog.mapper

import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.entity.Post
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PostMapper {
    fun toDto(post: Post): PostResponse

    fun toDtoList(posts: List<Post>): List<PostResponse>

    @Mapping(target = "viewCount", constant = "0")
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    fun toEntity(postRequest: PostRequest): Post
}

