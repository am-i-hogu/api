package com.hogu.am_i_hogu.domain.auth.service;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogoutService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    public LogoutService(JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository, TokenHasher tokenHasher) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public void logout(
            Authentication authentication,
            String refreshToken
    ) {
        RefreshToken savedRefreshToken = loadAndValidateRefreshToken(authentication, refreshToken);
        if (savedRefreshToken == null) {
            return;
        }

        savedRefreshToken.revoke(LocalDateTime.now());
    }

    /**
     * refresh token 검증
     * - 올바른 jwt refresh token인지 확인
     * - authentication에 들어있는 유저 정보와 일치하는 유저 정보를 담고 있는지 확인
     * - refresh token 정보가 DB에 저장되어 있는지 확인
     * - DB에 저장된 정보를 찾아 hash 값이 일치하는지 확인
     * - revoke된 refresh token인지 확인
     *
     * @param authentication    유저 인증 정보
     * @param refreshToken      요청으로 들어온 refresh token
     * @return 검증에 문제가 있는 경우 null, 문제가 없는 경우 DB에 저장되어 있었던 refresh token 정보
     */
    private RefreshToken loadAndValidateRefreshToken(
            Authentication authentication,
            String refreshToken
    ) {
        if (!isValidRefreshTokenRequest(refreshToken)) {
            return null;
        }
        if (!matchesAuthenticatedUser(authentication, refreshToken)) {
            return null;
        }

        Long refreshTokenId = jwtProvider.getTokenId(refreshToken);
        RefreshToken savedRefreshToken = refreshTokenRepository.findById(refreshTokenId)
                .orElse(null);

        if (savedRefreshToken == null) {
            return null;
        }
        if (!matchesStoredToken(savedRefreshToken, refreshToken)) {
            return null;
        }
        if (savedRefreshToken.isRevoked()) {
            return null;
        }

        return savedRefreshToken;
    }

    // 올바른 jwt refresh token인지 검증
    private boolean isValidRefreshTokenRequest(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }

        return jwtProvider.validateRefreshToken(refreshToken) == JwtProvider.TokenValidationResult.VALID;
    }

    // authentication에 들어있는 유저 정보와 일치하는 유저 정보를 담고 있는지 확인
    private boolean matchesAuthenticatedUser(Authentication authentication, String refreshToken) {
        if (authentication == null) {
            return true;
        }

        Long authenticatedUserId = Long.valueOf(authentication.getName());
        Long refreshTokenUserId = jwtProvider.getSubjectAsLong(refreshToken);

        return authenticatedUserId.equals(refreshTokenUserId);
    }

    // DB에 저장된 정보와 hash 값이 일치하는지 확인
    private boolean matchesStoredToken(RefreshToken savedRefreshToken, String refreshToken) {
        return savedRefreshToken.getTokenHash().equals(tokenHasher.hash(refreshToken));
    }
}
