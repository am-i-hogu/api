package com.hogu.am_i_hogu.domain.oauth.dto.response;

import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import lombok.Getter;

@Getter
public class OAuthAuthenticationResult {
    private final OAuthUserInfo userInfo;
    private final TokenResponse tokenResponse;

    public OAuthAuthenticationResult(
            OAuthUserInfo userInfo,
            TokenResponse tokenResponse
    ) {
        this.userInfo = userInfo;
        this.tokenResponse = tokenResponse;
    }
}
