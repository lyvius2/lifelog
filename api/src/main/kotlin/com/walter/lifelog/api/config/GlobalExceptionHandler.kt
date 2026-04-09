package com.walter.lifelog.api.config

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.shared.config.exception.AuthenticationException
import com.walter.lifelog.shared.config.exception.GoogleDriveException
import com.walter.lifelog.shared.config.exception.InvalidPhotoLikedException
import com.walter.lifelog.shared.config.exception.LoginException
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

    @ExceptionHandler(GoogleDriveException::class)
    fun handleGoogleDriveException(e: GoogleDriveException): ResponseEntity<Rest<Nothing>> {
        log.warn("GoogleDriveException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Rest(statusCode = 404, message = e.message!!))
    }

    @ExceptionHandler(InvalidPhotoLikedException::class)
    fun handleInvalidPhotoLikedException(e: InvalidPhotoLikedException): ResponseEntity<Rest<Nothing>> {
        log.warn("InvalidPhotoLikedException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Rest(statusCode = 400, message = e.message!!))
    }

    @ExceptionHandler(LoginException::class)
    fun handleLoginException(e: LoginException): ResponseEntity<LoginResponse> {
        log.error("LoginException: {}", e.message, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(LoginResponse(success = false, message = e.message!!))
    }

    @ExceptionHandler(value = [IllegalArgumentException::class, IllegalAccessException::class])
    fun handleIllegalException(e: Exception): ResponseEntity<Rest<Nothing>> {
        log.warn("IllegalException: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Rest(statusCode = 400, message = e.message ?: "잘못된 요청입니다."))
    }

    @ExceptionHandler(value = [Exception::class, RuntimeException::class])
    fun handleException(e: Exception): ResponseEntity<Rest<Nothing>> {
        log.error("Unhandled exception: {}", e.message, e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Rest(statusCode = 500, message = "서버 오류가 발생했습니다."))
    }
}

