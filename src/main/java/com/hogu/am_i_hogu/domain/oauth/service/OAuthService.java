package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.oauth.config.GoogleOAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.*;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthCallbackResult;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.repository.OAuthLoginStateRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class OAuthService {
    private final SecureRandom secureRandom = new SecureRandom();

    private final GoogleOAuthProperties googleOAuthProperties;
    private final String onboardingUri;
    private final String loginSuccessUri;
    private final OAuthCallbackHandlerFactory oauthCallbackHandlerFactory;
    private final OAuthLoginStateRepository oauthLoginStateRepository;
    private final TsidGenerator tsidGenerator;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegisterSessionRepository registerSessionRepository;

    public OAuthService(
            GoogleOAuthProperties googleOAuthProperties,
            @Value("${app.redirect.onboarding-uri}") String onboardingUri,
            @Value("${app.redirect.login-success-uri}") String loginSuccessUri,
            OAuthCallbackHandlerFactory oauthCallbackHandlerFactory,
            OAuthLoginStateRepository oauthLoginStateRepository,
            TsidGenerator tsidGenerator,
            SocialAccountRepository socialAccountRepository,
            JwtProvider jwtProvider,
            RefreshTokenRepository refreshTokenRepository,
            RegisterSessionRepository registerSessionRepository) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.onboardingUri = onboardingUri;
        this.loginSuccessUri = loginSuccessUri;
        this.oauthCallbackHandlerFactory = oauthCallbackHandlerFactory;
        this.oauthLoginStateRepository = oauthLoginStateRepository;
        this.tsidGenerator = tsidGenerator;
        this.socialAccountRepository = socialAccountRepository;
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registerSessionRepository = registerSessionRepository;
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
            case GOOGLE -> buildGoogleAuthorizationUrl();
        };
    }

    /**
     * google 소셜 로그인을 위한 authorization URL 생성
     * state, nonce 값을 생성하여 DB에 저장 후 URL에 포함
     *
     * @return 사용자를 redirect 시킬 google 로그인 페이지 URL
     */
    private String buildGoogleAuthorizationUrl() {
        String state = generateRandomValue();
        String nonce = generateRandomValue();
        saveOAuthLoginState(OAuthProvider.GOOGLE, state, nonce);

        return UriComponentsBuilder
                .fromUriString(googleOAuthProperties.getAuthorizationUri())
                .queryParam("client_id", googleOAuthProperties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", googleOAuthProperties.getScope())
                .queryParam("redirect_uri", googleOAuthProperties.getRedirectUri())
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .build()
                .toUriString();
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

        OAuthUserInfo oauthUserInfo = oauthCallbackHandlerFactory.get(provider)
                .handle(code, oauthLoginState);

        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(
                        oauthUserInfo.getProvider(),
                        oauthUserInfo.getProviderUserId()
                )
                .orElseGet(() -> createUnlinkedSocialAccount(oauthUserInfo, now));

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

        String refreshToken = jwtProvider.createRefreshToken(userId);
        RefreshToken savedRefreshToken = new RefreshToken(
                tsidGenerator.nextId(),
                userId,
                hashToken(refreshToken),
                false,
                false,
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
                hashToken(registerToken),
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
                null,
                oAuthUserInfo.getProvider(),
                oAuthUserInfo.getProviderUserId(),
                null,
                now
        );

        return socialAccountRepository.save(socialAccount);
    }

    /**
     * DB에 저장하기 위해 토큰 hashing
     *
     * @param token DB에 저장할 원본 토큰 문자열
     * @return SHA-256으로 해시한 토큰 문자열
     */
    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception e) {
            throw new CustomException(CommonErrorCode.SERVER_ERROR);
        }
    }
}
