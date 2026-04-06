package com.walter.lifelog.shared.config.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
    public AuthenticationException() {
        super("E-Mail 또는 비밀번호가 올바르지 않습니다.");
    }
}
