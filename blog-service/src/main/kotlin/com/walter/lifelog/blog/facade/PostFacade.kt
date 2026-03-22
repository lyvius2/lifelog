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
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.annotation.Facade
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.util.AsyncSupporter.asyncSupply
import com.walter.lifelog.shared.util.ViewCountHelper
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Facade
class PostFacade(
    private val categoryService: CategoryService,
    private val postService: PostService,
    private val postTagService: PostTagService,
    private val virtualThreadExecutor: TaskExecutor,
    private val viewCountHelper: ViewCountHelper,
    private val transactionTemplate: TransactionTemplate,
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
        val viewCount = viewCountHelper.increment("post_${post.postSeq}")
        return PostContents.of(post, viewCount, prevPostFuture.get(), nextPostFuture.get())
    }

    @DynamicCacheable(value = ["searchedPosts"], key = "#postSearchCondition?.toString() ?: 'empty'", ttlMinutes = 1)
    fun getSearchedPosts(postSearchCondition: PostSearchCondition?): PageResponse<PostListResponse> {
        if (postSearchCondition == null) {
            return postService.getSearchedPosts(PostSearchCondition())
        }
        return postService.getSearchedPosts(postSearchCondition)
    }

    fun savePost(postRequest: PostRequest, userSeq: Long) : PostSaveResponse {
        return transactionTemplate.execute { _ ->
            val post = postService.savePost(postRequest, userSeq)
            postTagService.savePostTag(post.postSeq!!, postRequest)
            PostSaveResponse.of(post)
        }
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

    fun getCategoryTree(): List<CategoryTreeResponse> {
        return categoryService.getCategoryTree()
    }
}