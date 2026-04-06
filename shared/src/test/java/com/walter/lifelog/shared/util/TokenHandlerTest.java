package com.walter.lifelog.shared.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccessTokenHandler 테스트")
class TokenHandlerTest {

    private static final String SECRET_KEY = "tempKey";

    @Test
    @DisplayName("accessToken 생성 후 파싱하면 동일한 Email이 추출된다")
    void generateAndParseToken_shouldExtractSameEmail() {
        // given
        String email = "admin@example.com";

        // when
        String accessToken = TokenHandler.generateAccessToken(email, SECRET_KEY);
        Claims claims = TokenHandler.parseToken(accessToken, SECRET_KEY);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(email);
    }

    @Test
    @DisplayName("accessToken 생성 시 userSeq, displayName이 클레임에 포함된다")
    void generateToken_shouldIncludeUserSeqAndDisplayName() {
        // given
        String email = "admin@example.com";
        Long userSeq = 123L;
        String displayName = "홍길동";

        // when
        String accessToken = TokenHandler.generateAccessToken(email, userSeq, displayName, SECRET_KEY);
        Claims claims = TokenHandler.parseToken(accessToken, SECRET_KEY);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.get("userSeq", Long.class)).isEqualTo(userSeq);
        assertThat(claims.get("displayName", String.class)).isEqualTo(displayName);
    }
}
