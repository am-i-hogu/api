package com.hogu.am_i_hogu.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JwtProviderTest {
    private static final String secretKey = "thisisthesecretkeyforJwtProviderTest";     // 테스트용 비밀키(32byte 이상)
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
}
