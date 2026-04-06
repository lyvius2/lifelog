package com.walter.lifelog.user.repository
import com.walter.lifelog.user.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenAndIsActiveTrue(token: String): RefreshToken?

    @Modifying
    @Query("UPDATE RefreshToken r SET r.isActive = false WHERE r.userSeq = :userSeq AND r.isActive = true")
    fun invalidateAllByUserSeq(@Param("userSeq") userSeq: Long): Int
}
