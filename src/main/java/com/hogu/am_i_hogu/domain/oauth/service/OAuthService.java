package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenEncryptor;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.auth.domain.RegisterSession;
import com.hogu.am_i_hogu.domain.oauth.config.OAuthClientProperties;
import com.hogu.am_i_hogu.domain.oauth.config.OAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.*;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthAuthenticationResult;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthCallbackResult;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.repository.OAuthLoginStateRepository;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.auth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialOAuthTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
public class OAuthService {
    private final SecureRandom secureRandom = new SecureRandom();

    private final OAuthProperties oauthProperties;
    private final String onboardingUri;
    private final String loginSuccessUri;
    private final OAuthCallbackHandler oauthCallbackHandler;
    private final OAuthLoginStateRepository oauthLoginStateRepository;
    private final TsidGenerator tsidGenerator;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegisterSessionRepository registerSessionRepository;
    private final TokenEncryptor tokenEncryptor;
    private final SocialOAuthTokenRepository socialOAuthTokenRepository;

    public OAuthService(
            OAuthProperties oauthProperties,
            @Value("${app.redirect.onboarding-uri}") String onboardingUri,
            @Value("${app.redirect.login-success-uri}") String loginSuccessUri,
            OAuthCallbackHandler oauthCallbackHandler,
            OAuthLoginStateRepository oauthLoginStateRepository,
            TsidGenerator tsidGenerator,
            SocialAccountRepository socialAccountRepository,
            JwtProvider jwtProvider,
            TokenHasher tokenHasher,
            RefreshTokenRepository refreshTokenRepository,
            RegisterSessionRepository registerSessionRepository,
            TokenEncryptor tokenEncryptor,
            SocialOAuthTokenRepository socialOAuthTokenRepository
    ) {
        this.oauthProperties = oauthProperties;
        this.onboardingUri = onboardingUri;
        this.loginSuccessUri = loginSuccessUri;
        this.oauthCallbackHandler = oauthCallbackHandler;
        this.oauthLoginStateRepository = oauthLoginStateRepository;
        this.tsidGenerator = tsidGenerator;
        this.socialAccountRepository = socialAccountRepository;
        this.jwtProvider = jwtProvider;
        this.tokenHasher = tokenHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registerSessionRepository = registerSessionRepository;
        this.tokenEncryptor = tokenEncryptor;
        this.socialOAuthTokenRepository = socialOAuthTokenRepository;
    }

    /**
     * 지정된 OAuth provider의 로그인 페이지 URL 생성하여 반환
     * state, nonce 생성 및 DB 기록
     *
     * @param provider 요청할 소셜 로그인 제공자
     * @return 사용자를 리다이렉트 시킬 로그인 페이지의 URL 문자열
     */
    public String getAuthorizationUrl(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> buildAuthorizationUrl(OAuthProvider.GOOGLE);
            case KAKAO -> buildAuthorizationUrl(OAuthProvider.KAKAO);
        };
    }

    /**
     * 소셜 로그인을 위한 authorization URL 생성
     * state, nonce 값을 생성하여 DB에 저장 후 URL에 포함
     *
     * @return 사용자를 redirect 시킬 로그인 페이지 URL
     */
    private String buildAuthorizationUrl(OAuthProvider provider) {
        String state = generateRandomValue();
        String nonce = generateRandomValue();
        saveOAuthLoginState(provider, state, nonce);

        OAuthClientProperties properties = oauthProperties.getClientProperties(provider);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getAuthorizationUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getScope())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .queryParam("nonce", nonce);

        Map<String, String> authorizationParams = properties.getAuthorizationParams();
        if (authorizationParams != null) {
            authorizationParams.forEach(builder::queryParam);
        }

        return builder.build().toUriString();
    }

    /**
     * state, nonce 값 생성을 위한 난수 생성기
     *
     * @return URL-Safe 32byte 난수 문자열
     */
    private String generateRandomValue() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /**
     * OAuthLoginState를 생성해 DB에 저장
     *
     * @param provider  소셜로그인 provider
     * @param state     CSRF 공격 방어를 위한 상태값
     * @param nonce     ID token 무결성 검증을 위한 난수값
     */
    private void saveOAuthLoginState(
            OAuthProvider provider,
            String state,
            String nonce
    ) {
        long id = tsidGenerator.nextId();

        OAuthLoginState oauthLoginState = new OAuthLoginState(
                id,
                provider,
                state,
                nonce,
                LocalDateTime.now()
        );

        oauthLoginStateRepository.save(oauthLoginState);
    }

    /**
     * 소셜 서버로부터 code, state를 받아
     * - state 검증
     * - code를 이용해 소셜 서버로부터 token 발급
     * - id-token 검증
     * - 기존/신규 회원 분기 처리
     * - 적절한 토큰 반환
     *
     * @param provider  소셜 로그인 provider
     * @param code      소셜 서버로부터 발급받은 authorization code
     * @param state     로그인 요청할 때 소셜 서버로 보냈던 state 값
     */
    @Transactional
    public OAuthCallbackResult handleCallback(OAuthProvider provider, String code, String state) {
        OAuthLoginState oauthLoginState = oauthLoginStateRepository.findByState(state)
                .orElseThrow(()->new CustomException(OAuthErrorCode.INVALID_STATE));

        LocalDateTime now = LocalDateTime.now();
        if (oauthLoginState.isConsumed()) {
            throw new CustomException(OAuthErrorCode.STATE_REUSED);
        }
        if (oauthLoginState.isExpired(now)) {
            throw new CustomException(OAuthErrorCode.STATE_EXPIRED);
        }

        OAuthAuthenticationResult authResult = oauthCallbackHandler.handle(
                code,
                oauthLoginState,
                provider
        );

        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        authResult.getUserInfo().getProvider(),
                        authResult.getUserInfo().getProviderUserId()
                )
                .orElseGet(() -> createUnlinkedSocialAccount(authResult.getUserInfo(), now));

        saveSocialOAuthToken(socialAccount.getId(), authResult.getTokenResponse(), now);

        OAuthCallbackResult result = socialAccount.isLinked()
                ? handleExistingUser(socialAccount, now)
                : handleNewUser(socialAccount, now);

        oauthLoginState.markConsumed(now);
        oauthLoginStateRepository.save(oauthLoginState);

        return result;
    }

    /**
     * 기존 회원인 경우 refresh token 발급
     *
     * @param socialAccount 기존 회원과 연결된 social account 정보
     * @param now           refresh token 발급 시각
     * @return 로그인 성공 후 redirect/cookie 응답 정보
     */
    private OAuthCallbackResult handleExistingUser(SocialAccount socialAccount, LocalDateTime now) {
        Long userId = socialAccount.getUserId();
        Long refreshTokenId = tsidGenerator.nextId();

        String refreshToken = jwtProvider.createRefreshToken(userId, refreshTokenId);
        RefreshToken savedRefreshToken = new RefreshToken(
                refreshTokenId,
                userId,
                tokenHasher.hash(refreshToken),
                now
        );
        refreshTokenRepository.save(savedRefreshToken);

        return new OAuthCallbackResult(
                loginSuccessUri,
                "refreshToken",
                refreshToken
        );
    }

    /**
     * 신규 회원인 경우 register token 발급
     *
     * @param socialAccount 신규 회원의 social account 정보
     * @param now           register token 발급 시각
     * @return 온보딩 진행을 위한 redirect/cookie 응답 정보
     */
    private OAuthCallbackResult handleNewUser(SocialAccount socialAccount, LocalDateTime now) {
        String registerToken = jwtProvider.createRegisterToken(socialAccount.getId());
        RegisterSession registerSession = new RegisterSession(
                tsidGenerator.nextId(),
                socialAccount.getId(),
                tokenHasher.hash(registerToken),
                now
        );
        registerSessionRepository.save(registerSession);

        return new OAuthCallbackResult(
                onboardingUri,
                "registerToken",
                registerToken
        );
    }

    /**
     * 신규 회원인 경우 social account table에 정보 생성
     *
     * @param oAuthUserInfo 소셜 서버로부터 확인한 사용자 식별 정보
     * @param now           social account 생성 시각
     * @return DB에 저장된 social account 객체
     */
    private SocialAccount createUnlinkedSocialAccount(OAuthUserInfo oAuthUserInfo, LocalDateTime now) {
        SocialAccount socialAccount = new SocialAccount(
                tsidGenerator.nextId(),
                oAuthUserInfo.getProvider(),
                oAuthUserInfo.getProviderUserId(),
                now
        );

        return socialAccountRepository.save(socialAccount);
    }

    private void saveSocialOAuthToken(
            Long socialAccountId,
            TokenResponse tokenResponse,
            LocalDateTime now
    ) {
        if (tokenResponse == null || !hasText(tokenResponse.getAccessToken()) || tokenResponse.getExpiresIn() == null) {
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }

        String encryptedAccessToken = tokenEncryptor.encrypt(tokenResponse.getAccessToken());
        LocalDateTime accessTokenExpiresAt = now.plusSeconds(tokenResponse.getExpiresIn());

        SocialOAuthToken savedToken = socialOAuthTokenRepository.findBySocialAccountId(socialAccountId)
                .orElse(null);

        if (savedToken == null) {
            if (!hasText(tokenResponse.getRefreshToken())) {
                throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
            }

            SocialOAuthToken newToken = new SocialOAuthToken(
                    tsidGenerator.nextId(),
                    socialAccountId,
                    encryptedAccessToken,
                    tokenEncryptor.encrypt(tokenResponse.getRefreshToken()),
                    accessTokenExpiresAt,
                    toRefreshTokenExpiresAt(now, tokenResponse.getRefreshTokenExpiresIn()),
                    now
            );
            socialOAuthTokenRepository.save(newToken);
            return;
        }

        String encryptedRefreshToken = savedToken.getRefreshTokenEncrypted();
        LocalDateTime refreshTokenExpiresAt = savedToken.getRefreshTokenExpiresAt();

        if (hasText(tokenResponse.getRefreshToken())) {
            encryptedRefreshToken = tokenEncryptor.encrypt(tokenResponse.getRefreshToken());
            refreshTokenExpiresAt = toRefreshTokenExpiresAt(now, tokenResponse.getRefreshTokenExpiresIn());
        }

        savedToken.updateTokens(
                encryptedAccessToken,
                encryptedRefreshToken,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                now
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime toRefreshTokenExpiresAt(LocalDateTime now, Integer refreshTokenExpiresIn) {
        if (refreshTokenExpiresIn == null) {            // google은 refresh_token_expires_in 반환하지 않음
            return null;
        }
        return now.plusSeconds(refreshTokenExpiresIn);
    }
}
