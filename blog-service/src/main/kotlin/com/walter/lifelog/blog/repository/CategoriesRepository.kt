package com.walter.lifelog.blog.repository

import com.walter.lifelog.blog.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CategoriesRepository : JpaRepository<Category, Long> {
    @Query("select c from Category c where c.isActive = true order by c.categorySeq asc")
    fun findInActive(): List<Category>
}