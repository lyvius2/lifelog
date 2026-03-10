package com.walter.lifelog.photo.repository

import com.walter.lifelog.photo.entity.Photo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PhotosRepository : JpaRepository<Photo, Long> {
}