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
    private final OAuthClient oauthClient;
    private final IdTokenVerifier idTokenVerifier;

    public GoogleOAuthCallbackHandler(
            OAuthClient oauthClient,
            IdTokenVerifier idTokenVerifier
    ) {
        this.oauthClient = oauthClient;
        this.idTokenVerifier = idTokenVerifier;
    }

    @Override
    public OAuthProvider supports() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo handle(String code, OAuthLoginState oauthLoginState) {
        TokenResponse tokenResponse = oauthClient.requestToken(code, OAuthProvider.GOOGLE);
        String idToken = getIdToken(tokenResponse);
        Jwt jwt = idTokenVerifier.verify(idToken, oauthLoginState.getNonce());

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
