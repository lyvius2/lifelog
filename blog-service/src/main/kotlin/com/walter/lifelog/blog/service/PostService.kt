package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSimpleInfo
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.blog.mapper.PostMapper
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.shared.config.exception.PostNotFoundException
import com.walter.lifelog.shared.util.MarkdownConverter
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostService(
    private val postsRepository: PostsRepository,
    private val postMapper: PostMapper,
) {
    fun getPost(inquiryStr: String): PostResponse {
        val post = if (inquiryStr.toLongOrNull() != null) {
            postsRepository.findByPostSeq(inquiryStr.toLong())
        } else {
            postsRepository.findBySlug(inquiryStr)
        }
        if (post == null) {
            throw PostNotFoundException(inquiryStr)
        }
        return postMapper.toDto(post)
    }

    fun getPrevPostInfo(categorySeq: Long, createdAt: LocalDateTime): PostSimpleInfo? {
        return postsRepository.findPrevPost(categorySeq, createdAt)?.let { postMapper.toPostSimpleInfoDto(it) }
    }

    fun getNextPostInfo(categorySeq: Long, createdAt: LocalDateTime): PostSimpleInfo? {
        return postsRepository.findNextPost(categorySeq, createdAt)?.let { postMapper.toPostSimpleInfoDto(it) }
    }

    fun savePost(postRequest: PostRequest, userSeq: Long) : PostResponse {
        val content = MarkdownConverter.convert(postRequest.markdownContent)
        postRequest.apply {
            this.content = content
            this.userSeq = userSeq
        }
        val postEntity = postMapper.toEntity(postRequest)
        if (postRequest.status == PostStatus.PUBLISHED.name) {
            postEntity.publishedAt = LocalDateTime.now()
        }
        return postMapper.toDto(postsRepository.save(postEntity))
    }
}