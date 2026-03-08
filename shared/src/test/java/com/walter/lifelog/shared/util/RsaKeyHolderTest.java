package com.walter.lifelog.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RsaKeyHolder 테스트")
class RsaKeyHolderTest {

    @Test
    @DisplayName("공개키로 암호화한 문자열을 decrypt로 복호화하면 원본과 동일하다")
    void decrypt_shouldReturnOriginalPlainText() throws Exception {
        // given
        String plainText = "mySecretPassword123!";
        String encrypted = encryptWithPublicKey(plainText);

        // when
        String decrypted = RsaKeyHolder.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("한글이 포함된 비밀번호도 정상적으로 복호화된다")
    void decrypt_shouldHandleKoreanCharacters() throws Exception {
        // given
        String plainText = "비밀번호123!@#";
        String encrypted = encryptWithPublicKey(plainText);

        // when
        String decrypted = RsaKeyHolder.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("잘못된 Base64 문자열을 복호화하면 RuntimeException이 발생한다")
    void decrypt_shouldThrowExceptionForInvalidInput() {
        // given
        String invalidEncrypted = "invalidBase64String!!";

        // when & then
        assertThatThrownBy(() -> RsaKeyHolder.decrypt(invalidEncrypted))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("RSA 복호화 실패");
    }

    @Test
    @DisplayName("getPublicKeyBase64는 비어있지 않은 Base64 문자열을 반환한다")
    void getPublicKeyBase64_shouldReturnNonEmptyString() {
        // when
        String publicKey = RsaKeyHolder.getPublicKeyBase64();

        // then
        assertThat(publicKey).isNotBlank();
        assertThat(Base64.getDecoder().decode(publicKey)).isNotEmpty();
    }

    @Test
    @DisplayName("매 호출마다 동일한 공개키를 반환한다")
    void getPublicKeyBase64_shouldReturnSameKeyOnMultipleCalls() {
        // when
        String firstCall = RsaKeyHolder.getPublicKeyBase64();
        String secondCall = RsaKeyHolder.getPublicKeyBase64();

        // then
        assertThat(firstCall).isEqualTo(secondCall);
    }

    private String encryptWithPublicKey(String plainText) throws Exception {
        String publicKeyBase64 = RsaKeyHolder.getPublicKeyBase64();
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
