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

    private boolean isValidRefreshTokenRequest(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }

        return jwtProvider.validateRefreshToken(refreshToken) == JwtProvider.TokenValidationResult.VALID;
    }

    private boolean matchesAuthenticatedUser(Authentication authentication, String refreshToken) {
        if (authentication == null) {
            return true;
        }

        Long authenticatedUserId = Long.valueOf(authentication.getName());
        Long refreshTokenUserId = jwtProvider.getSubjectAsLong(refreshToken);

        return authenticatedUserId.equals(refreshTokenUserId);
    }

    private boolean matchesStoredToken(RefreshToken savedRefreshToken, String refreshToken) {
        return savedRefreshToken.getTokenHash().equals(tokenHasher.hash(refreshToken));
    }
}
