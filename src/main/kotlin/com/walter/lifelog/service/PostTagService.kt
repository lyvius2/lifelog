package com.walter.lifelog.service

import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.entity.PostTag
import com.walter.lifelog.repository.PostTagsRepository
import org.springframework.stereotype.Service

@Service
class PostTagService(
    private val postTagsRepository: PostTagsRepository,
) {
    fun getTags(postSeq: Long): List<String> {
        return postTagsRepository.findByPostSeq(postSeq).map { it.tag }
    }

    fun savePostTag(postSeq: Long, postRequest: PostRequest) {
        postTagsRepository.deleteByPostSeq(postSeq)
        var index = 0
        postRequest.tags?.forEach { tag ->
            val postTag = PostTag(postSeq = postSeq, tagSeq = index++, tag = tag)
            postTagsRepository.save(postTag)
        }
    }
}