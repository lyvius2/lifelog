package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.entity.PhotoTagId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PhotoTagRepository : JpaRepository<PhotoTag, PhotoTagId> {
    fun findByPhotoSeq(photoSeq: Long): List<PhotoTag>
    fun deleteByPhotoSeq(photoSeq: Long)
}