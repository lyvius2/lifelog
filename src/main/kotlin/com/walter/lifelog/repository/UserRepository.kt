package com.walter.lifelog.repository

import com.walter.lifelog.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByUserSeq(userSeq: Long): User?
}

