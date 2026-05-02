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
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String REGISTER_TOKEN_TYPE = "register";

    private static final Long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 20;
    private static final Long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;
    private static final Long REGISTER_TOKEN_EXPIRATION = 1000L * 60 * 10;

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
    public String createAccessToken(Long userId) {
        return createToken(String.valueOf(userId), ACCESS_TOKEN_EXPIRATION, ACCESS_TOKEN_TYPE);
    }

    // userId, refreshTokenId 이용해 Refresh Token 생성
    public String createRefreshToken(Long userId, Long refreshTokenId) {
        return createToken(
                String.valueOf(userId),
                REFRESH_TOKEN_EXPIRATION,
                REFRESH_TOKEN_TYPE,
                String.valueOf(refreshTokenId)
        );
    }

    // socialAccountId 이용해 Register Token 생성
    public String createRegisterToken(Long socialAccountId) {
        return createToken(String.valueOf(socialAccountId), REGISTER_TOKEN_EXPIRATION, REGISTER_TOKEN_TYPE);
    }

    // access token, register token 생성 (token id 사용하지 않음)
    private String createToken(String subject, long expirationTime, String tokenType) {
        return createToken(subject, expirationTime, tokenType, null);
    }

    // refresh token 생성 (token id 사용)
    private String createToken(String subject, long expirationTime, String tokenType, String tokenId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        var builder = Jwts.builder()
                .subject(subject)                   // (1) 토큰 주인 식별자
                .claim(TOKEN_TYPE_CLAIM, tokenType) // (2) 토큰 용도 구분
                .issuedAt(now)                      // (3) 발행 일시
                .expiration(expiration)             // (4) 만료 일시
                .signWith(secretKey);               // (5) 비밀키로 서명

        if (tokenId != null) {
            builder.id(tokenId);
        }

        return builder.compact();                   // (6) 직렬화
    }

    // Access Token 검증
    public TokenValidationResult validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return TokenValidationResult.EMPTY;
        }
        try {
            String tokenType = Jwts.parser()
                    .verifyWith(secretKey)              // (1) 검증키 설정
                    .build()
                    .parseSignedClaims(accessToken)     // (2) 토큰 해석, 서명 검증
                    .getPayload()
                    .get(TOKEN_TYPE_CLAIM, String.class);

            if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
                return TokenValidationResult.INVALID;
            }

            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;
        }
    }

    // register token 검증
    public TokenValidationResult validateRegisterToken(String registerToken) {
        if (registerToken == null || registerToken.isBlank()) {
            return TokenValidationResult.EMPTY;
        }

        try {
            String tokenType = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(registerToken)
                    .getPayload()
                    .get(TOKEN_TYPE_CLAIM, String.class);

            if (!REGISTER_TOKEN_TYPE.equals(tokenType)) {
                return TokenValidationResult.INVALID;
            }

            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;
        }
    }

    // refresh token 검증
    public TokenValidationResult validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return TokenValidationResult.EMPTY;
        }

        try {
            String tokenType = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload()
                    .get(TOKEN_TYPE_CLAIM, String.class);

            if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                return TokenValidationResult.INVALID;
            }

            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;
        }
    }

    public String getTokenType(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(TOKEN_TYPE_CLAIM, String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get(TOKEN_TYPE_CLAIM, String.class);
        }
    }

    public boolean isAccessTokenType(String tokenType) {
        return ACCESS_TOKEN_TYPE.equals(tokenType);
    }

    public boolean isRegisterTokenType(String tokenType) {
        return REGISTER_TOKEN_TYPE.equals(tokenType);
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

    /**
     * token의 subject를 얻는 메서드로,
     * - access token: userId
     * - refresh token: userId
     * - register token: socialAccountId
     * 를 얻을 수 있음
     *
     * @param token subject를 얻을 token
     * @return token에서 추출한 subject (Long 타입)
     */
    public Long getSubjectAsLong(String token) {
        String subject = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(subject);
    }

    // refresh token에서 jti를 얻음
    public Long getTokenId(String token) {
        String tokenId = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();

        return Long.valueOf(tokenId);
    }
}
