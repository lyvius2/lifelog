package com.walter.lifelog.api.config

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.content.config.exception.StaticPagePathConflictException
import com.walter.lifelog.content.dto.StaticPageResponse
import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.LoginException
import com.walter.lifelog.shared.config.exception.UnauthorizedException
import com.walter.lifelog.user.dto.LoginResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(e: AuthenticationException): ResponseEntity<LoginResponse> {
        log.warn("AuthenticationException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(LoginResponse(success = false, message = e.message!!))
    }

    @ExceptionHandler(LoginException::class)
    fun handleLoginException(e: LoginException): ResponseEntity<LoginResponse> {
        log.error("LoginException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LoginResponse(success = false, message = e.message!!))
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(e: UnauthorizedException): ResponseEntity<Rest<Nothing>> {
        log.warn("UnauthorizedException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Rest(statusCode = 401, message = e.message ?: "로그인이 필요합니다."))
    }

    /** 경로 충돌은 잘못된 입력이 아니라 "덮어쓸지 물어볼 거리" 이므로 409 로 내려보냅니다. */
    @ExceptionHandler(StaticPagePathConflictException::class)
    fun handleStaticPagePathConflict(e: StaticPagePathConflictException): ResponseEntity<Rest<StaticPageResponse>> {
        log.warn("StaticPagePathConflictException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Rest(statusCode = 409, message = e.message!!, data = e.existingPage))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Rest<Nothing>> {
        log.warn("IllegalArgumentException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Rest(statusCode = 400, message = e.message ?: "잘못된 요청입니다."))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Rest<Nothing>> {
        log.error("Unhandled exception: {}", e.message, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Rest(statusCode = 500, message = "서버 오류가 발생했습니다."))
    }
}

