package com.hogu.am_i_hogu.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtProvider {

    // 토큰 상태 구분을 위한 enum
    public enum TokenValidationResult {
        VALID,      // 유효한 토큰
        EXPIRED,    // 만료된 토큰
        INVALID     // 잘못된 토큰
    }
    private final SecretKey secretKey;

    // secret key 값을 가져와 Key 객체로 변환
    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // userId 이용해 Access Token 생성
    public String createAccessToken(Long userId, long accessTokenExpirationTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationTime);

        return Jwts.builder()
                .subject(String.valueOf(userId))    // (1) 토큰 주인 식별자
                .issuedAt(now)                      // (2) 발행 일시
                .expiration(expiration)             // (3) 만료 일시
                .signWith(secretKey)                // (4) 비밀키로 서명
                .compact();                         // (5) 직렬화
    }

    public TokenValidationResult validateAccessToken(String accessToken) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)              // (1) 검증키 설정
                .build()
                .parseSignedClaims(accessToken)     // (2) 토큰 해석, 서명 검증
                .getPayload();                      // (3) 토큰의 데이터 추출(userId)
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;

        }
    }
}
