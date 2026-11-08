package com.walter.lifelog.user.repository

import com.walter.lifelog.user.entity.User
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * users 테이블 접근. jOOQ 로 직접 매핑합니다.
 */
@Repository
class UserRepository(
    private val dsl: DSLContext,
) {
    companion object {
        private val USERS = DSL.table("users")

        private val USER_SEQ = DSL.field("user_seq", Long::class.java)
        private val EMAIL = DSL.field("email", String::class.java)
        private val NAME = DSL.field("name", String::class.java)
        private val PASSWORD_HASH = DSL.field("password_hash", String::class.java)
        private val DISPLAY_NAME = DSL.field("display_name", String::class.java)
        private val BIO = DSL.field("bio", String::class.java)
        private val PROFILE_IMAGE_URL = DSL.field("profile_image_url", String::class.java)
        private val GITHUB_URL = DSL.field("github_url", String::class.java)
        private val LINKEDIN_URL = DSL.field("linkedin_url", String::class.java)
        private val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
        private val UPDATED_AT = DSL.field("updated_at", LocalDateTime::class.java)
        private val IS_ACTIVE = DSL.field("is_active", Boolean::class.java)

        private val ALL_FIELDS = arrayOf(
            USER_SEQ, EMAIL, NAME, PASSWORD_HASH, DISPLAY_NAME, BIO,
            PROFILE_IMAGE_URL, GITHUB_URL, LINKEDIN_URL, CREATED_AT, UPDATED_AT, IS_ACTIVE,
        )
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): User? = findOne(EMAIL.eq(email))

    @Transactional(readOnly = true)
    fun findByUserSeq(userSeq: Long): User? = findOne(USER_SEQ.eq(userSeq))

    private fun findOne(condition: Condition): User? {
        return dsl.select(*ALL_FIELDS)
            .from(USERS)
            .where(condition)
            .limit(1)
            .fetchOne()
            ?.let { toUser(it) }
    }

    private fun toUser(record: Record): User = User(
        userSeq = record.get(USER_SEQ),
        email = record.get(EMAIL) ?: "",
        name = record.get(NAME) ?: "",
        passwordHash = record.get(PASSWORD_HASH) ?: "",
        displayName = record.get(DISPLAY_NAME) ?: "",
        bio = record.get(BIO),
        profileImageUrl = record.get(PROFILE_IMAGE_URL),
        githubUrl = record.get(GITHUB_URL),
        linkedinUrl = record.get(LINKEDIN_URL),
        createdAt = record.get(CREATED_AT),
        updatedAt = record.get(UPDATED_AT),
        isActive = record.get(IS_ACTIVE) ?: true,
    )
}
