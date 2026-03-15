package com.walter.lifelog.config

import org.assertj.core.api.Assertions.assertThat
import org.jasypt.encryption.StringEncryptor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JasyptConfig 암복호화 테스트")
class JasyptConfigTest {

    private lateinit var encryptor: StringEncryptor

    @BeforeEach
    fun setUp() {
        val jasyptConfig = JasyptConfig("lifelogENC")
        encryptor = jasyptConfig.stringEncryptor()
    }

    @Test
    @DisplayName("평문을 암호화하면 원문과 다른 문자열이 생성된다")
    fun encrypt_shouldProduceDifferentString() {
        // given
        val plainText = "mySecretPassword123!"

        // when
        val encrypted = encryptor.encrypt(plainText)

        // then
        println("평문: $plainText")
        println("암호문: $encrypted")
        assertThat(encrypted).isNotBlank()
        assertThat(encrypted).isNotEqualTo(plainText)
    }

    @Test
    @DisplayName("암호화된 문자열을 복호화하면 원문과 동일하다")
    fun decrypt_shouldReturnOriginalPlainText() {
        // given
        val plainText = "mySecretPassword123!"
        val encrypted = encryptor.encrypt(plainText)

        // when
        val decrypted = encryptor.decrypt(encrypted)

        // then
        println("평문: $plainText")
        println("암호문: $encrypted")
        println("복호문: $decrypted")
        assertThat(decrypted).isEqualTo(plainText)
    }

    @Test
    @DisplayName("동일한 평문을 두 번 암호화해도 서로 다른 암호문이 생성된다 (RandomSalt)")
    fun encrypt_shouldProduceDifferentCiphertextEachTime() {
        // given
        val plainText = "sameText"

        // when
        val encrypted1 = encryptor.encrypt(plainText)
        val encrypted2 = encryptor.encrypt(plainText)

        // then
        println("암호문1: $encrypted1")
        println("암호문2: $encrypted2")
        assertThat(encrypted1).isNotEqualTo(encrypted2)
        assertThat(encryptor.decrypt(encrypted1)).isEqualTo(plainText)
        assertThat(encryptor.decrypt(encrypted2)).isEqualTo(plainText)
    }

    @Test
    @DisplayName("빈 문자열도 암복호화가 가능하다")
    fun encrypt_shouldHandleEmptyString() {
        // given
        val plainText = ""

        // when
        val encrypted = encryptor.encrypt(plainText)
        val decrypted = encryptor.decrypt(encrypted)

        // then
        assertThat(decrypted).isEqualTo(plainText)
    }

    @Test
    @DisplayName("한글, 특수문자가 포함된 문자열도 암복호화가 가능하다")
    fun encrypt_shouldHandleKoreanAndSpecialChars() {
        // given
        val plainText = "한글패스워드!@#\$%^&*()"

        // when
        val encrypted = encryptor.encrypt(plainText)
        val decrypted = encryptor.decrypt(encrypted)

        // then
        println("평문: $plainText")
        println("암호문: $encrypted")
        println("복호문: $decrypted")
        assertThat(decrypted).isEqualTo(plainText)
    }
}

