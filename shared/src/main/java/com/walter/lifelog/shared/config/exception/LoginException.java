package com.walter.lifelog.shared.config.exception;

public class LoginException extends RuntimeException {
    public LoginException(Throwable cause) {
        super("인증 처리 중 오류 발생 : " + cause.getMessage(), cause);
    }
    public LoginException() {
        super("인증 처리 중 오류가 발생하였습니다.");
    }
}
