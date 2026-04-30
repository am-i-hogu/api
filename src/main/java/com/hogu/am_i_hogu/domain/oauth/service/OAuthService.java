package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.oauth.config.GoogleOAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.repository.OAuthLoginStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class OAuthService {
    private final SecureRandom secureRandom = new SecureRandom();

    private final GoogleOAuthProperties googleOAuthProperties;
    private final OAuthLoginStateRepository oauthLoginStateRepository;
    private final TsidGenerator tsidGenerator;

    public OAuthService(
            GoogleOAuthProperties googleOAuthProperties,
            OAuthLoginStateRepository oauthLoginStateRepository,
            TsidGenerator tsidGenerator) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.oauthLoginStateRepository = oauthLoginStateRepository;
        this.tsidGenerator = tsidGenerator;
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
}
