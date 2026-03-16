package com.walter.lifelog.blog.dto

data class PostContents(
    val content: PostResponse?,
    val viewCount: Long,
    val prevContent: PostSimpleInfo?,
    val nextContent: PostSimpleInfo?,
) {
    companion object {
        @JvmStatic
        fun of(content: PostResponse, viewCount: Long, prevContent: PostSimpleInfo?, nextContent: PostSimpleInfo?): PostContents {
            return PostContents(content, viewCount, prevContent, nextContent)
        }
    }

    fun getWriterUserSeq(): Long? {
        return content?.userSeq
    }
}
