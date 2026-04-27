package com.hogu.am_i_hogu.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JwtProviderTest {
    private static final String secretKey =                                             // 테스트용 Base64 인코딩 secret
            "5VQQMfjGcREAKxUDV+5OTdFkrFl8L7521GqCeJVesE7ZsKbAUQLk6K45dQkwHmf2jJmpbaqODszgk0uKB3NziQ==";
    private static final long accessTokenExpirationTime = 1000L * 60 * 30;              // 만료 시간 30분

    private final JwtProvider jwtProvider = new JwtProvider(secretKey);

    /**
     * Access Token 생성 테스트:
     * 토큰을 발급한 뒤 비어있지 않은지 확인
     */
    @Test
    void createAccessTokenTest() {
        String accessToken = jwtProvider.createAccessToken(1L, accessTokenExpirationTime);
        assertThat(accessToken).isNotBlank();
    }

    /**
     * 유효한 Access Token 검증 테스트:
     * 토큰을 발급한 뒤 즉시 검증해 VALID를 리턴하는지 확인
     */
    @Test
    void validateValidAccessTokenTest() {
        String accessToken = jwtProvider.createAccessToken(1L, accessTokenExpirationTime);
        JwtProvider.TokenValidationResult result = jwtProvider.validateAccessToken(accessToken);
        assertThat(result).isEqualTo(JwtProvider.TokenValidationResult.VALID);
    }

    /**
     * 만료된 Access Token 검증 테스트:
     * 만료 시간이 1ms인 토큰 발급해 10ms 뒤 검증
     * EXPIRED를 리턴하는지 확인
     */
    @Test
    void validateExpiredAccessTokenTest() throws InterruptedException {
        String accessToken = jwtProvider.createAccessToken(1L, 1L);
        Thread.sleep(10L);
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
        String accessToken = jwtProvider.createAccessToken(1L, accessTokenExpirationTime);
        Authentication authentication = jwtProvider.getAuthentication(accessToken);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("1");
    }
}
