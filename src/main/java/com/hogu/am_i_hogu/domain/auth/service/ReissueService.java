package com.hogu.am_i_hogu.domain.auth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.auth.dto.response.TokenPair;
import com.hogu.am_i_hogu.domain.auth.exception.AuthErrorCode;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReissueService {
    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final TokenIssueService tokenIssueService;
    private final RefreshTokenRepository refreshTokenRepository;

    public ReissueService(
            JwtProvider jwtProvider,
            TokenHasher tokenHasher,
            TokenIssueService tokenIssueService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.tokenHasher = tokenHasher;
        this.tokenIssueService = tokenIssueService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // 토큰 재발급
    @Transactional
    public TokenPair reissueToken(String refreshToken) {
        RefreshToken savedRefreshToken = loadAndValidateRefreshToken(refreshToken);

        LocalDateTime now = LocalDateTime.now();
        savedRefreshToken.markRotated();
        savedRefreshToken.revoke(now);

        return tokenIssueService.issueTokenPair(savedRefreshToken.getUserId(), now);
    }

    // refresh token 검증
    private RefreshToken loadAndValidateRefreshToken(String refreshToken) {
        // JWT 레벨 검증
        JwtProvider.TokenValidationResult validationResult =
                jwtProvider.validateRefreshToken(refreshToken);

        if (validationResult == JwtProvider.TokenValidationResult.EMPTY) {
            throw new CustomException(AuthErrorCode.EMPTY_REFRESH_TOKEN);
        }
        if (validationResult == JwtProvider.TokenValidationResult.EXPIRED) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (validationResult == JwtProvider.TokenValidationResult.INVALID) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // DB 저장값 검증
        Long refreshTokenId = jwtProvider.getTokenId(refreshToken);
        RefreshToken savedRefreshToken = refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (!savedRefreshToken.getTokenHash().equals(tokenHasher.hash(refreshToken))) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (savedRefreshToken.isRevoked() || savedRefreshToken.isRotated()) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        return savedRefreshToken;
    }
}
