package com.walter.lifelog.web.config;

import com.walter.lifelog.shared.config.exception.PageViewNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(annotations = Controller.class)
public class WebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(PageViewNotExistException.class)
    public String handlePageViewNotExistException(PageViewNotExistException e) {
        log.warn("PageViewNotExistException: {}", e.getMessage());
        return "not-found";
    }
}

