package com.walter.lifelog.blog.dto

data class PostContents(
    val content: PostResponse?,
    val prevContent: PostSimpleInfo?,
    val nextContent: PostSimpleInfo?,
) {
    companion object {
        @JvmStatic
        fun of(content: PostResponse, prevContent: PostSimpleInfo?, nextContent: PostSimpleInfo?): PostContents {
            return PostContents(content, prevContent, nextContent)
        }
    }

    fun getWriterUserSeq(): Long? {
        return content?.userSeq
    }
}
