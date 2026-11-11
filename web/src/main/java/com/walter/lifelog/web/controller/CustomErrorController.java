package com.walter.lifelog.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class CustomErrorController {
    /*
      뷰 분기는 ModelAndView 로 상태 코드까지 지정합니다. 예전에는 뷰 이름만 돌려줘
      안내 페이지가 HTTP 200 으로 나갔습니다 — 브라우저에는 같아 보여도 크롤러와
      API 클라이언트에는 "정상 응답" 으로 보입니다.
    */
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
        return new ModelAndView("not-found", HttpStatus.NOT_FOUND);
    }
}
