package com.walter.lifelog.shared.config.exception;

/**
 * 인증 없이 보호된 리소스에 접근했을 때 던집니다.
 *
 * <p>자격 증명이 틀린 경우({@link AuthenticationException}) 와 달리, 자격 증명 자체가
 * 없거나 만료된 경우입니다. 둘 다 401 이지만 메시지가 다릅니다.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
