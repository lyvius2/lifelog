package com.walter.lifelog.shared.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccessTokenHandler 테스트")
class AccessTokenHandlerTest {

    private static final String SECRET_KEY = "tempKey";

    @Test
    @DisplayName("accessToken 생성 후 파싱하면 동일한 Email이 추출된다")
    void generateAndParseToken_shouldExtractSameEmail() {
        // given
        String email = "admin@example.com";

        // when
        String accessToken = AccessTokenHandler.generateToken(email, SECRET_KEY);
        Claims claims = AccessTokenHandler.parseToken(accessToken, SECRET_KEY);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(email);
    }
}
