package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.entity.PhotoCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PhotoCategoryRepository : JpaRepository<PhotoCategory, Long> {
    @Query("select p from PhotoCategory p where p.isActive = true")
    fun findAllActiveCategories(): List<PhotoCategory>
}