package com.walter.lifelog.worker.log.database.repository

import com.walter.lifelog.worker.log.database.entity.PhotoLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PhotosLogRepository : JpaRepository<PhotoLog, Long>