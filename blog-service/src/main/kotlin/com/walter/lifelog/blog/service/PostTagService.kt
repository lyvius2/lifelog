package com.walter.lifelog.blog.service

import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.entity.PostTag
import com.walter.lifelog.blog.repository.PostTagsRepository
import com.walter.lifelog.shared.annotation.DynamicCacheable
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

@Service
class PostTagService(
    private val postTagsRepository: PostTagsRepository,
) {
    @DynamicCacheable(value = ["tags"], key = "#postSeq", ttlMinutes = 1440)
    fun getTags(postSeq: Long): List<String> {
        return postTagsRepository.findByPostSeq(postSeq).map { it.tag }
    }

    @CacheEvict(value = ["tags"], key = "#postSeq")
    fun savePostTag(postSeq: Long, postRequest: PostRequest) {
        postTagsRepository.deleteByPostSeq(postSeq)
        var index = 0
        postRequest.tags?.forEach { tag ->
            val postTag = PostTag(postSeq = postSeq, tagSeq = index++, tag = tag)
            postTagsRepository.save(postTag)
        }
    }
}