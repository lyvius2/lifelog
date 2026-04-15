package com.walter.lifelog.shared.config.exception;

public class PageViewNotExistException extends RuntimeException {
    public PageViewNotExistException() {
        super("존재하지 않는 웹페이지입니다.");
    }
}
