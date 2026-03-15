package com.walter.lifelog.blog.facade

import com.walter.lifelog.blog.dto.CategoryTreeResponse
import com.walter.lifelog.blog.dto.PostContents
import com.walter.lifelog.blog.dto.PostEditorContents
import com.walter.lifelog.blog.dto.PostListResponse
import com.walter.lifelog.blog.dto.PostRequest
import com.walter.lifelog.blog.dto.PostResponse
import com.walter.lifelog.blog.dto.PostSaveResponse
import com.walter.lifelog.blog.dto.PostSearchCondition
import com.walter.lifelog.blog.service.CategoryService
import com.walter.lifelog.blog.service.PostService
import com.walter.lifelog.blog.service.PostTagService
import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.annotation.Transactional

@Facade
class PostFacade(
    private val categoryService: CategoryService,
    private val postService: PostService,
    private val postTagService: PostTagService,
    private val virtualThreadExecutor: TaskExecutor,
) {
    @Transactional(readOnly = true)
    fun getPost(inquiryStr: String) : PostResponse {
        return createPostResponse(inquiryStr)
    }

    @Transactional(readOnly = true)
    fun getPostContents(inquiryStr: String) : PostContents {
        val post = createPostResponse(inquiryStr)
        val prevPostFuture = asyncSupply(virtualThreadExecutor) { postService.getPrevPostInfo(post.categorySeq!!, post.createdAt!!) }
        val nextPostFuture = asyncSupply(virtualThreadExecutor) { postService.getNextPostInfo(post.categorySeq!!, post.createdAt!!) }
        return PostContents.of(post, prevPostFuture.get(), nextPostFuture.get())
    }

    @Transactional(readOnly = true)
    fun getSearchedPosts(postSearchCondition: PostSearchCondition?): PageResponse<PostListResponse> {
        if (postSearchCondition == null) {
            return postService.getSearchedPosts(PostSearchCondition())
        }
        return postService.getSearchedPosts(postSearchCondition)
    }

    @Transactional
    fun savePost(postRequest: PostRequest, userSeq: Long) : PostSaveResponse {
        val post = postService.savePost(postRequest, userSeq)
        postTagService.savePostTag(post.postSeq!!, postRequest)
        return PostSaveResponse.of(post)
    }

    @Transactional(readOnly = true)
    fun getPostEditorContents() : PostEditorContents {
        return PostEditorContents.of(categoryService.getActiveCategories(), PostResponse.empty())
    }

    @Transactional(readOnly = true)
    fun getPostEditorContents(postSeq: Long) : PostEditorContents {
        val post = createPostResponse(postSeq.toString())
        val postCategorySeq = post.categorySeq
        val categories = categoryService.getActiveCategories()
        categories.filter { it.categorySeq == postCategorySeq }.map { it.isChecked = true }
        return PostEditorContents.of(categories, post)
    }

    private fun createPostResponse(inquiryStr: String): PostResponse {
        val post = postService.getPost(inquiryStr)
        val tags = postTagService.getTags(post.postSeq!!)
        post.apply {
            this.tags = tags
        }
        return post
    }

    @Transactional(readOnly = true)
    fun getCategoryTree(): List<CategoryTreeResponse> {
        return categoryService.getCategoryTree()
    }
}