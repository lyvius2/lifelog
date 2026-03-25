package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.entity.Photo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PhotosRepository : JpaRepository<Photo, Long> {
    fun findTopByPhotoSeq(photoSeq: Long): Photo?

    @Query("SELECT p FROM Photo p WHERE p.shotAt IS NOT NULL AND p.isActive = true ORDER BY p.shotAt ASC LIMIT 1")
    fun findEarliestShot(): Photo?

    @Query("SELECT p FROM Photo p WHERE p.shotAt IS NOT NULL AND p.isActive = true ORDER BY p.shotAt DESC LIMIT 1")
    fun findLatestShot(): Photo?
}