package com.walter.lifelog.shared.util;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

public final class RsaKeyHolder {
    private static final KeyPair KEY_PAIR;

    static {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("RSA 키 쌍 생성 실패", e);
        }
    }

    private RsaKeyHolder() {}

    public static String getPublicKeyBase64() {
        final PublicKey publicKey = KEY_PAIR.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static String decrypt(String encryptedBase64) {
        try {
            final PrivateKey privateKey = KEY_PAIR.getPrivate();
            final Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            final OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            final byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("RSA 복호화 실패", e);
        }
    }
}
