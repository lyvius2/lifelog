package com.walter.lifelog.api.util

import com.walter.lifelog.shared.config.exception.InvalidPhotoLikedException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class CookieHandler {
    companion object {
        private const val LIKED_PHOTOS_COOKIE = "liked_photos"
        private const val LIKE_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24시간

        @JvmStatic
        fun validateCookie(photoSeq: Long, request: HttpServletRequest, response: HttpServletResponse) {
            val likedPhotos = getLikedPhotosFromCookie(request)
            val now = System.currentTimeMillis()
            val likedAt = likedPhotos[photoSeq]
            if (likedAt != null && now - likedAt < LIKE_COOLDOWN_MS) {
                throw InvalidPhotoLikedException("24시간 이내에 이미 좋아요를 누른 사진입니다.")
            }

            likedPhotos[photoSeq] = now
            likedPhotos.entries.removeIf { now - it.value >= LIKE_COOLDOWN_MS }
            val cookieValue = likedPhotos.entries.joinToString("|") { "${it.key}:${it.value}" }
            val cookie = Cookie(LIKED_PHOTOS_COOKIE, cookieValue)
            cookie.path = "/"
            cookie.maxAge = 60 * 60 * 24 * 2 // 2일
            cookie.isHttpOnly = true
            response.addCookie(cookie)
        }

        private fun getLikedPhotosFromCookie(request: HttpServletRequest): MutableMap<Long, Long> {
            val cookie = request.cookies?.find { it.name == LIKED_PHOTOS_COOKIE } ?: return mutableMapOf()
            val now = System.currentTimeMillis()
            return cookie.value
                .split("|")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val seq = parts[0].trim().toLongOrNull()
                        val timestamp = parts[1].trim().toLongOrNull()
                        if (seq != null && timestamp != null && now - timestamp < LIKE_COOLDOWN_MS) {
                            seq to timestamp
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .toMap()
                .toMutableMap()
        }
    }
}