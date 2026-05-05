package com.hogu.am_i_hogu.domain.auth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.auth.dto.response.TokenPair;
import com.hogu.am_i_hogu.domain.auth.exception.AuthErrorCode;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.auth.service.ReissueService;
import com.hogu.am_i_hogu.domain.auth.service.TokenIssueService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

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
    void emptyRefreshTokenTest() {
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
    void expiredRefreshTokenTest() {
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
    void invalidRefreshTokenTest() {
        when(jwtProvider.validateRefreshToken("invalid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);

        assertThatThrownBy(() -> reissueService.reissueToken("invalid-refresh-token"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    /**
     * 저장된 refresh token이 존재하지 않는 경우 테스트:
     * refresh token의 token id로 DB에서 값을 찾지 못하면
     * INVALID_REFRESH_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void refreshTokenNotFoundTest() {
        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);
        when(refreshTokenRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reissueService.reissueToken("valid-refresh-token"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    /**
     * refresh token hash가 일치하지 않는 경우 테스트:
     * DB에 저장된 refresh token hash와 요청 token hash가 다르면
     * INVALID_REFRESH_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void refreshTokenHashMismatchTest() {
        RefreshToken refreshToken = new RefreshToken(
                100L,
                10L,
                "saved-hash",
                LocalDateTime.now()
        );

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);
        when(refreshTokenRepository.findById(100L))
                .thenReturn(Optional.of(refreshToken));
        when(tokenHasher.hash("valid-refresh-token"))
                .thenReturn("different-hash");

        assertThatThrownBy(() -> reissueService.reissueToken("valid-refresh-token"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    /**
     * 이미 사용된 refresh token 테스트:
     * rotated 처리된 refresh token으로 재발급 요청 시
     * REFRESH_TOKEN_REUSED 예외가 발생하는지 확인
     */
    @Test
    void reusedRefreshTokenTest() {
        RefreshToken refreshToken = new RefreshToken(
                100L,
                10L,
                "saved-hash",
                LocalDateTime.now()
        );
        refreshToken.markRotated();

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);
        when(refreshTokenRepository.findById(100L))
                .thenReturn(Optional.of(refreshToken));
        when(tokenHasher.hash("valid-refresh-token"))
                .thenReturn("saved-hash");

        assertThatThrownBy(() -> reissueService.reissueToken("valid-refresh-token"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSED));
    }

    /**
     * 토큰 재발급 성공 테스트:
     * - 유효한 refresh token과 저장된 refresh token 정보를 준비하고,
     * - (1) 기존 refresh token이 rotated, revoked 처리되는지 확인
     * - (2) TokenIssueService가 호출되는지 확인
     * - (3) access token과 refresh token이 반환되는지 확인
     */
    @Test
    void reissueTokenSuccessTest() {
        RefreshToken refreshToken = new RefreshToken(
                100L,
                10L,
                "saved-hash",
                LocalDateTime.now()
        );

        when(jwtProvider.validateRefreshToken("valid-refresh-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getTokenId("valid-refresh-token"))
                .thenReturn(100L);
        when(refreshTokenRepository.findById(100L))
                .thenReturn(Optional.of(refreshToken));
        when(tokenHasher.hash("valid-refresh-token"))
                .thenReturn("saved-hash");
        when(tokenIssueService.issueTokenPair(eq(10L), any(LocalDateTime.class)))
                .thenReturn(new TokenPair("new-access-token", "new-refresh-token"));

        TokenPair result = reissueService.reissueToken("valid-refresh-token");

        assertThat(refreshToken.isRotated()).isTrue();
        assertThat(refreshToken.isRevoked()).isTrue();
        verify(tokenIssueService).issueTokenPair(eq(10L), any(LocalDateTime.class));
        verify(refreshTokenRepository, never()).save(any());
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
    }
}
