package com.walter.lifelog.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class AccessTokenHandler {
    private static final long EXPIRATION_MILLIS = 24 * 60 * 60 * 1000L; // 24시간

    public static String generateToken(String email, String secretKey) {
        return generateToken(email, null, null, secretKey);
    }

    public static String generateToken(String email, Long userSeq, String displayName, String secretKey) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MILLIS);

        var builder = Jwts.builder()
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
