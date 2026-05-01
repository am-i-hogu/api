package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * google OAuth callback 처리 담당
 */
@Component
public class GoogleOAuthCallbackHandler implements OAuthCallbackHandler {
    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleOAuthCallbackHandler(
            GoogleOAuthClient googleOAuthClient,
            GoogleIdTokenVerifier googleIdTokenVerifier
    ) {
        this.googleOAuthClient = googleOAuthClient;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    @Override
    public OAuthProvider supports() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo handle(String code, OAuthLoginState oauthLoginState) {
        TokenResponse tokenResponse = googleOAuthClient.requestToken(code);
        String idToken = getIdToken(tokenResponse);
        Jwt jwt = googleIdTokenVerifier.verify(idToken, oauthLoginState.getNonce());

        return new OAuthUserInfo(OAuthProvider.GOOGLE, jwt.getSubject());
    }

    private String getIdToken(TokenResponse tokenResponse) {
        if (tokenResponse == null
                || tokenResponse.getIdToken() == null
                || tokenResponse.getIdToken().isBlank()) {
            throw new CustomException(OAuthErrorCode.INVALID_ID_TOKEN);
        }
        return tokenResponse.getIdToken();
    }
}
