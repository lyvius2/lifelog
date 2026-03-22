package com.walter.lifelog.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {
    @Test
    @DisplayName("BCryptPasswordEncoder를 사용하여 비밀번호를 인코딩하고 검증하는 테스트")
    void encodePassword() {
        // given
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin1234";

        // when
        String encodedPassword = encoder.encode(rawPassword);

        // then
        System.out.println("===== BCrypt Password Encoding =====");
        System.out.println("Raw Password    : " + rawPassword);
        System.out.println("Encoded Password: " + encodedPassword);
        System.out.println("Matches         : " + encoder.matches(rawPassword, encodedPassword));
        System.out.println("====================================");
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }
}

