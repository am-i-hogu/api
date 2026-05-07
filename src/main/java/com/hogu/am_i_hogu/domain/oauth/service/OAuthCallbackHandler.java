package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthAuthenticationResult;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * google OAuth callback 처리 담당
 */
@Component
public class OAuthCallbackHandler {
    private final OAuthClient oauthClient;
    private final IdTokenVerifier idTokenVerifier;

    public OAuthCallbackHandler(
            OAuthClient oauthClient,
            IdTokenVerifier idTokenVerifier
    ) {
        this.oauthClient = oauthClient;
        this.idTokenVerifier = idTokenVerifier;
    }

    public OAuthAuthenticationResult handle(
            String code,
            OAuthLoginState oauthLoginState,
            OAuthProvider provider
    ) {
        TokenResponse tokenResponse = oauthClient.requestToken(code, provider);
        String idToken = getIdToken(tokenResponse);

        Jwt jwt = idTokenVerifier.verify(
                idToken,
                oauthLoginState.getNonce(),
                provider
        );

        OAuthUserInfo userInfo = new OAuthUserInfo(provider, jwt.getSubject());
        return new OAuthAuthenticationResult(userInfo, tokenResponse);
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
