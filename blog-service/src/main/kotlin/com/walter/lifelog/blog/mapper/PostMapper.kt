package com.walter.lifelog.blog.mapper

import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.shared.util.MarkdownConverter
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = [MarkdownConverter::class])
interface PostMapper {
    @Mapping(target = "categoryName", source = "category.categoryName")
    @Mapping(target = "userName", source = "user.displayName")
    @Mapping(
        target = "content",
        expression = "java(post.getContent() != null && !post.getContent().isEmpty() ? post.getContent() : com.walter.lifelog.shared.util.MarkdownConverter.convert(post.getMarkdownContent()))"
    )
    fun toDto(post: Post): PostResponse

    fun toDtoList(posts: List<Post>): List<PostResponse>

    @Mapping(target = "viewCount", constant = "0")
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    fun toEntity(postRequest: PostRequest): Post
}

