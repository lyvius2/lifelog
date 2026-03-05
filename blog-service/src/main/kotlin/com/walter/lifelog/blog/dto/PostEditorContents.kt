package com.walter.lifelog.blog.dto

data class PostEditorContents(
    val categories: List<PostCategory>,
    val content: PostResponse,
) {
    companion object {
        @JvmStatic
        fun of(categories: List<PostCategory>, content: PostResponse): PostEditorContents {
            return PostEditorContents(categories, content)
        }
    }
}