package com.walter.lifelog.user.repository

import com.walter.lifelog.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByUserSeq(userSeq: Long): User?
}