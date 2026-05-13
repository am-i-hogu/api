package com.hogu.am_i_hogu.domain.auth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.domain.auth.exception.AuthErrorCode;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ReissueServiceTest {
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final TokenHasher tokenHasher = mock(TokenHasher.class);
    private final TokenIssueService tokenIssueService = mock(TokenIssueService.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final ReissueService reissueService = new ReissueService(
            jwtProvider,
            tokenHasher,
            tokenIssueService,
            refreshTokenRepository
    );

    /**
     * refresh token이 비어있는 경우 테스트:
     * refresh token 없이 재발급 요청 시
     * EMPTY_REFRESH_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void reissueTokenThrowsEmptyRefreshTokenWhenRefreshTokenIsEmpty() {
        when(jwtProvider.validateRefreshToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        assertThatThrownBy(() -> reissueService.reissueToken(null))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMPTY_REFRESH_TOKEN));
    }

    /**
     * refresh token이 만료된 경우 테스트:
     * 만료된 refresh token으로 재발급 요청 시
     * REFRESH_TOKEN_EXPIRED 예외가 발생하는지 확인
     */
    @Test
    void reissueTokenThrowsRefreshTokenExpiredWhenRefreshTokenIsExpired() {
        when(jwtProvider.validateRefreshToken("expired-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.EXPIRED);

        assertThatThrownBy(() -> reissueService.reissueToken("expired-refresh-token"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED));
    }

    /**
     * refresh token이 잘못된 경우 테스트:
     * 잘못된 refresh token으로 재발급 요청 시
     * INVALID_REFRESH_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void reissueTokenThrowsInvalidRefreshTokenWhenRefreshTokenIsInvalid() {
        when(jwtProvider.validateRefreshToken("invalid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);

        assertThatThrownBy(() -> reissueService.reissueToken("invalid-refresh-token"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }
}
