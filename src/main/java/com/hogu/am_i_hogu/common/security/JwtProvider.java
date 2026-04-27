package com.hogu.am_i_hogu.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtProvider {

    // 토큰 상태 구분을 위한 enum
    public enum TokenValidationResult {
        VALID,      // 유효한 토큰
        EXPIRED,    // 만료된 토큰
        INVALID,    // 잘못된 토큰
        EMPTY       // 토큰 비어있음
    }
    private final SecretKey secretKey;

    // secret key 값을 가져와 Base64 decode 후 Key 객체로 변환
    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
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

    // Access Token 검증
    public TokenValidationResult validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return TokenValidationResult.EMPTY;
        }
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

    // Access Token 해독하여 Authentication으로 변환
    public Authentication getAuthentication(String accessToken) {
        String userId = Jwts.parser()
                .verifyWith(secretKey)              // (1) 검증키 설정
                .build()
                .parseSignedClaims(accessToken)     // (2) 토큰 해석
                .getPayload()                       // (3) 토큰 payload 얻어옴
                .getSubject();                      // (4) 토큰 subject 얻어옴

        // Authentication 객체 생성하여 반환
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.emptyList()
        );
    }
}
