package com.hogu.am_i_hogu.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JwtProviderTest {
    private static final String secretKey =                                             // 테스트용 Base64 인코딩 secret
            "5VQQMfjGcREAKxUDV+5OTdFkrFl8L7521GqCeJVesE7ZsKbAUQLk6K45dQkwHmf2jJmpbaqODszgk0uKB3NziQ==";

    private final JwtProvider jwtProvider = new JwtProvider(secretKey);

    /**
     * Access Token 생성 테스트:
     * 토큰을 발급한 뒤 비어있지 않은지 확인
     */
    @Test
    void createAccessTokenTest() {
        String accessToken = jwtProvider.createAccessToken(1L);
        assertThat(accessToken).isNotBlank();
    }

    /**
     * 유효한 Access Token 검증 테스트:
     * 토큰을 발급한 뒤 즉시 검증해 VALID를 리턴하는지 확인
     */
    @Test
    void validateValidAccessTokenTest() {
        String accessToken = jwtProvider.createAccessToken(1L);
        JwtProvider.TokenValidationResult result = jwtProvider.validateAccessToken(accessToken);
        assertThat(result).isEqualTo(JwtProvider.TokenValidationResult.VALID);
    }

    /**
     * 만료된 Access Token 검증 테스트:
     * 이미 만료된 access token을 생성해 검증했을 때
     * EXPIRED를 리턴하는지 확인
     */
    @Test
    void validateExpiredAccessTokenTest() {
        SecretKey signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
        Date now = new Date();
        String accessToken = Jwts.builder()
                .subject("1")
                .claim("type", "access")
                .issuedAt(new Date(now.getTime() - 2000L))
                .expiration(new Date(now.getTime() - 1000L))
                .signWith(signingKey)
                .compact();

        JwtProvider.TokenValidationResult result = jwtProvider.validateAccessToken(accessToken);
        assertThat(result).isEqualTo(JwtProvider.TokenValidationResult.EXPIRED);
    }

    /**
     * 잘못된 Access Token 검증 테스트:
     * JWT 형식이 아닌 임의의 문자열을 검증
     * INVALID를 리턴하는지 확인
     */
    @Test
    void validateInvalidAccessTokenTest() {
        JwtProvider.TokenValidationResult result = jwtProvider.validateAccessToken("invalid-access-token");
        assertThat(result).isEqualTo(JwtProvider.TokenValidationResult.INVALID);
    }

    /**
     * 비어있는 Access Token 검증 테스트:
     * 공백으로 이루어진 문자열을 검증
     * EMPTY를 리턴하는지 확인
     */
    @Test
    void validateEmptyAccessTokenTest() {
        JwtProvider.TokenValidationResult result = jwtProvider.validateAccessToken(" ");
        assertThat(result).isEqualTo(JwtProvider.TokenValidationResult.EMPTY);
    }

    /**
     * Authentication 객체 생성 테스트:
     * userId 1로 access token 발급
     * access token 해독하여 Authentication 생성
     * Authentication의 principal이 1인지 확인
     */
    @Test
    void getAuthentication() {
        String accessToken = jwtProvider.createAccessToken(1L);
        Authentication authentication = jwtProvider.getAuthentication(accessToken);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("1");
    }

    /**
     * Refresh Token 식별자 추출 테스트:
     * refresh token 발급 후 token id를 추출해
     * 발급 시 넣은 식별자와 같은지 확인
     */
    @Test
    void getTokenIdTest() {
        String refreshToken = jwtProvider.createRefreshToken(1L, 100L);

        Long tokenId = jwtProvider.getTokenId(refreshToken);

        assertThat(tokenId).isEqualTo(100L);
    }
}
