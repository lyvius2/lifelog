package com.walter.lifelog.facade

import com.walter.lifelog.annotation.Facade
import com.walter.lifelog.controller.dto.PostRequest
import com.walter.lifelog.controller.dto.PostResponse
import com.walter.lifelog.controller.dto.PostSaveResponse
import com.walter.lifelog.mapper.PostMapper
import com.walter.lifelog.service.CategoryService
import com.walter.lifelog.service.PostService
import com.walter.lifelog.service.PostTagService
import com.walter.lifelog.service.UserService
import com.walter.lifelog.util.AccessTokenHandler
import jakarta.servlet.http.HttpSession
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture

@Facade
class PostFacade(
    private val categoryService: CategoryService,
    private val postService: PostService,
    private val postMapper: PostMapper,
    private val postTagService: PostTagService,
    private val userService: UserService,
    private val virtualThreadExecutor: TaskExecutor,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    fun validateAuthor(authorization: String?, session: HttpSession?) : Long {
        if (authorization != null) {
            val claims = AccessTokenHandler.validateAndParseToken(authorization, jwtSecretKey)
            val user = userService.getUserByEmail(claims.subject)
            if (user == null) {
                throw IllegalStateException("유효하지 않은 토큰입니다.")
            }
            return user.userSeq!!
        }
        return session!!.getAttribute("userSeq") as? Long
            ?: throw IllegalStateException("로그인이 필요합니다.")
    }

    @Transactional(readOnly = true)
    fun getPost(inquiryStr: String) : PostResponse {
        return createPostResponse(inquiryStr)
    }

    @Transactional(readOnly = true)
    fun getPostInfo(inquiryStr: String) : Map<String, Any?> {
        val post = createPostResponse(inquiryStr)
        val prevPostFuture = asyncSupply { postService.getPreviousPost(post.categorySeq!!, post.createdAt!!) }
        val nextPostFuture = asyncSupply { postService.getNextPost(post.categorySeq!!, post.createdAt!!) }
        val authorFuture = asyncSupply { userService.getAuthorInfoByUserSeq(post.userSeq!!) }
        return mapOf(
            "content" to post,
            "prevContent" to prevPostFuture.get(),
            "nextContent" to nextPostFuture.get(),
            "author" to authorFuture.get()
        )
    }

    @Transactional
    fun savePost(postRequest: PostRequest, userSeq: Long) : PostSaveResponse {
        val post = postService.savePost(postRequest, userSeq)
        postTagService.savePostTag(post.postSeq!!, postRequest)
        return PostSaveResponse(postSeq = post.postSeq!!, title = post.title)
    }

    fun getEditPost() : Map<String, Any> {
        return mapOf(
            "categories" to categoryService.getActiveCategories(),
            "content" to PostResponse(title = StringUtils.EMPTY, content = StringUtils.EMPTY)
        )
    }

    @Transactional(readOnly = true)
    fun getEditPost(postSeq: Long) : Map<String, Any> {
        val post = createPostResponse(postSeq.toString())

        val postCategorySeq = post.categorySeq
        val categories = categoryService.getActiveCategories()
        categories.filter { it.categorySeq == postCategorySeq }
            .map { it.isChecked = true }
        return mapOf(
            "categories" to categories,
            "content" to post
        )
    }

    private fun <T> asyncSupply(supplier: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(supplier, virtualThreadExecutor)

    private fun createPostResponse(inquiryStr: String): PostResponse {
        val postEntity = postService.getPost(inquiryStr)
        val tags = postTagService.getTags(postEntity.postSeq!!)
        val post = postMapper.toDto(postEntity).apply {
            this.tags = tags
        }
        return post
    }
}