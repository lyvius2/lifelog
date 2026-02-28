package com.walter.lifelog.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordEncoderTest {

    @Test
    @DisplayName("BCryptPasswordEncoder를 사용하여 비밀번호를 인코딩하고 검증하는 테스트")
    fun encodePassword() {
        // given
        val encoder = BCryptPasswordEncoder()
        val rawPassword = "admin1234"

        // when
        val encodedPassword = encoder.encode(rawPassword)

        // then
        println("===== BCrypt Password Encoding =====")
        println("Raw Password    : $rawPassword")
        println("Encoded Password: $encodedPassword")
        println("Matches         : ${encoder.matches(rawPassword, encodedPassword)}")
        println("====================================")
        assert(encoder.matches(rawPassword, encodedPassword))
    }
}

