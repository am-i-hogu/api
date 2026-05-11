package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.TokenEncryptor;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.auth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.domain.SocialAccount;
import com.hogu.am_i_hogu.domain.oauth.domain.SocialOAuthToken;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialOAuthTokenRepository;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.UserHoguStatRepository;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserDeletionService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialOAuthTokenRepository socialOAuthTokenRepository;
    private final TokenEncryptor tokenEncryptor;
    private final OAuthClient oauthClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegisterSessionRepository registerSessionRepository;
    private final UserHoguStatRepository userHoguStatRepository;

    public UserDeletionService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            SocialOAuthTokenRepository socialOAuthTokenRepository,
            TokenEncryptor tokenEncryptor,
            OAuthClient oauthClient,
            RefreshTokenRepository refreshTokenRepository,
            RegisterSessionRepository registerSessionRepository,
            UserHoguStatRepository userHoguStatRepository
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.socialOAuthTokenRepository = socialOAuthTokenRepository;
        this.tokenEncryptor = tokenEncryptor;
        this.oauthClient = oauthClient;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registerSessionRepository = registerSessionRepository;
        this.userHoguStatRepository = userHoguStatRepository;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        SocialAccount socialAccount = socialAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        SocialOAuthToken socialOAuthToken = socialOAuthTokenRepository.findBySocialAccountId(socialAccount.getId())
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        String accessToken = tokenEncryptor.decrypt(socialOAuthToken.getAccessTokenEncrypted());
        String refreshToken = tokenEncryptor.decrypt(socialOAuthToken.getRefreshTokenEncrypted());

        switch (socialAccount.getProvider()) {
            case GOOGLE -> unlinkGoogle(
                    accessToken,
                    refreshToken,
                    socialOAuthToken.getAccessTokenExpiresAt()
            );
            case KAKAO -> unlinkKakao(
                    accessToken,
                    refreshToken,
                    socialOAuthToken.getAccessTokenExpiresAt(),
                    socialOAuthToken.getRefreshTokenExpiresAt()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        registerSessionRepository.deleteAllBySocialAccountId(socialAccount.getId());
        socialOAuthTokenRepository.delete(socialOAuthToken);
        refreshTokenRepository.deleteAllByUserId(userId);
        userHoguStatRepository.deleteById(userId);
        socialAccountRepository.delete(socialAccount);

        user.delete(now);
    }

    private void unlinkGoogle(
            String accessToken,
            String refreshToken,
            LocalDateTime accessTokenExpiresAt
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            oauthClient.revokeGoogleToken(refreshToken);
            return;
        }
        if (accessToken != null
                && !accessToken.isBlank()
                && !isExpired(accessTokenExpiresAt)) {
            oauthClient.revokeGoogleToken(accessToken);
            return;
        }

        throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
    }

    private void unlinkKakao(
            String accessToken,
            String refreshToken,
            LocalDateTime accessTokenExpiresAt,
            LocalDateTime refreshTokenExpiresAt
    ) {
        if (isExpired(accessTokenExpiresAt)) {
            if (isExpired(refreshTokenExpiresAt)) {
                throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
            }

            reissueAndRevoke(refreshToken);
            return;
        }

        try {
            oauthClient.unlinkKakao(accessToken);
        } catch (CustomException e) {
            reissueAndRevoke(refreshToken);
        }
    }

    private void reissueAndRevoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(CommonErrorCode.SERVER_ERROR);
        }

        TokenResponse reissuedToken = oauthClient.reissueKakaoToken(refreshToken);

        if (reissuedToken == null
                || reissuedToken.getAccessToken() == null
                || reissuedToken.getAccessToken().isBlank()) {
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }

        oauthClient.unlinkKakao(reissuedToken.getAccessToken());
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return (expiresAt != null && !expiresAt.isAfter(now));
    }
}
