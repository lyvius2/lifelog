package com.walter.lifelog.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class CustomErrorController {
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handleNotFound(HttpServletRequest request, Exception e) {
        final String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api")) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "timestamp", LocalDateTime.now(),
                            "statusCode", 404,
                            "success", false,
                            "message", "요청한 API를 찾을 수 없습니다."
                    ));
        }
        return "not-found";
    }
}
