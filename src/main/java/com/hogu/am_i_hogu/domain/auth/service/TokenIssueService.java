package com.hogu.am_i_hogu.domain.auth.service;

import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.auth.dto.response.TokenPair;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenIssueService {
    private final TsidGenerator tsidGenerator;
    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenIssueService(
            TsidGenerator tsidGenerator,
            JwtProvider jwtProvider,
            TokenHasher tokenHasher,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.tsidGenerator = tsidGenerator;
        this.jwtProvider = jwtProvider;
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // access token, refresh token 발급
    public TokenPair issueTokenPair(Long userId, LocalDateTime createdAt) {
        Long refreshTokenId = tsidGenerator.nextId();
        String refreshToken = jwtProvider.createRefreshToken(userId, refreshTokenId);
        RefreshToken refreshTokenEntity = new RefreshToken(
                refreshTokenId,
                userId,
                tokenHasher.hash(refreshToken),
                createdAt
        );
        refreshTokenRepository.save(refreshTokenEntity);

        String accessToken = jwtProvider.createAccessToken(userId);

        return new TokenPair(accessToken, refreshToken);
    }
}
