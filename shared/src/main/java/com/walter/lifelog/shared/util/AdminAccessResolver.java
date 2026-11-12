package com.walter.lifelog.shared.util;

import java.util.Optional;

/**
 * 요청이 로그인한 관리자의 것인지 판별합니다.
 *
 * <p>api 모듈의 AdminRequiredAspect 와 web 모듈의 뷰 컨트롤러가 같은 규칙을 써야 하는데
 * web 은 api 에 의존하지 않으므로 규칙을 여기 한 곳에 둡니다. 서블릿 타입 대신 실제로
 * 쓰는 두 값만 받으므로 shared 가 웹 계층에 묶이지 않습니다.
 */
public class AdminAccessResolver {

    private AdminAccessResolver() {
    }

    /** Authorization 헤더를 먼저 보고, 없으면 세션 값을 씁니다. */
    public static Optional<Long> resolveUserSeq(String authorizationHeader, Object sessionUserSeq, String jwtSecretKey) {
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            return resolveFromToken(authorizationHeader, jwtSecretKey);
        }
        return resolveFromSession(sessionUserSeq);
    }

    public static Optional<Long> resolveFromToken(String authorizationHeader, String jwtSecretKey) {
        try {
            return Optional.ofNullable(TokenHandler.getUserSeqFromToken(authorizationHeader, jwtSecretKey));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Long> resolveFromSession(Object sessionUserSeq) {
        if (sessionUserSeq instanceof Number number) {
            return Optional.of(number.longValue());
        }
        return Optional.empty();
    }
}
