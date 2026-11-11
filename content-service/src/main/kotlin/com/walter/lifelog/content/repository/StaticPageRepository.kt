package com.walter.lifelog.content.repository

import com.walter.lifelog.content.entity.StaticPage
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StaticPageRepository : MongoRepository<StaticPage, String> {
    fun findByPathAndIsActiveTrue(path: String): StaticPage?

    /** unique index 와 같은 범위를 봐야 하므로 isActive 를 따지지 않습니다. */
    fun findByPath(path: String): StaticPage?

    fun existsByPath(path: String): Boolean

    fun findAllByIsActiveTrueOrderByCreatedAtDesc(): List<StaticPage>
}
