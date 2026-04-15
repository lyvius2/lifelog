package com.walter.lifelog.blog.mapper

import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSimpleInfo
import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.shared.util.MarkdownConverter
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import java.time.LocalDateTime

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = [MarkdownConverter::class, LocalDateTime::class, PostStatus::class])
interface PostMapper {
    @Mapping(target = "categoryName", source = "category.categoryName")
    @Mapping(target = "content", expression = "java(resolveContent(post))")
    @Mapping(target = "status", expression = "java(post.getStatus().name())")
    fun toDto(post: Post): PostResponse

    fun toDtoList(posts: List<Post>): List<PostResponse>

    fun toPostSimpleInfoDto(post: Post): PostSimpleInfo

    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "publishedAt", expression = "java(resolvePublishedAt(postRequest))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    fun toEntity(postRequest: PostRequest): Post

    fun resolveContent(post: Post): String? {
        return if (!post.content.isNullOrEmpty()) {
            post.content
        } else {
            MarkdownConverter.convert(post.markdownContent)
        }
    }

    fun resolvePublishedAt(postRequest: PostRequest): LocalDateTime? {
        if (postRequest.status != PostStatus.PUBLISHED.name) {
            return null
        }
        val publishedAt = postRequest.publishedAt
        return if (!publishedAt.isNullOrBlank()) {
            LocalDateTime.parse(publishedAt)
        } else {
            LocalDateTime.now()
        }
    }
}

