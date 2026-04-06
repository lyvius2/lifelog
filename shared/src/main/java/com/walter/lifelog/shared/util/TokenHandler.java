package com.walter.lifelog.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

public class TokenHandler {
    public static final long EXPIRATION_MILLIS = 10 * 60 * 1000L;  // 10분
    public static final int TOKEN_LENGTH = 64;
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateAccessToken(String email, String secretKey) {
        return generateAccessToken(email, null, null, secretKey);
    }

    public static String generateAccessToken(String email, Long userSeq, String displayName, String secretKey) {
        final Date now = new Date();
        final Date expiration = new Date(now.getTime() + EXPIRATION_MILLIS);
        final JwtBuilder builder = Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiration);
        if (userSeq != null) {
            builder.claim("userSeq", userSeq);
        }
        if (displayName != null) {
            builder.claim("displayName", displayName);
        }
        return builder
                .signWith(toSecretKey(secretKey))
                .compact();
    }

    public static String generateRefreshToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static Long getUserSeqFromToken(String token, String secretKey) throws IllegalAccessException {
        final Claims claims = validateAndParseToken(token, secretKey);
        final Object userSeqObj = claims.get("userSeq");
        if (userSeqObj instanceof Number) {
            return ((Number) userSeqObj).longValue();
        }
        return null;
    }

    public static Claims validateAndParseToken(String token, String secretKey) throws IllegalAccessException {
        final String bearerPrefix = "Bearer ";
        if (!token.startsWith(bearerPrefix)) {
            throw new IllegalAccessException("Authorization 헤더가 Bearer 타입이 아닙니다.");
        }
        final String accessToken = token.substring(bearerPrefix.length());
        return parseToken(accessToken, secretKey);
    }

    public static Claims parseToken(String token, String secretKey) {
        return Jwts.parser()
                .verifyWith(toSecretKey(secretKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey toSecretKey(String secret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
