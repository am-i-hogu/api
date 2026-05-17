package com.hogu.am_i_hogu.domain.oauth.dto.response;

import lombok.Getter;

/**
 * OAuth callback 처리 후 controller에 전달할 응답 정보
 */
@Getter
public class OAuthCallbackResult {
    private final String redirectUri;
    private final String cookieName;
    private final String cookieValue;

    public OAuthCallbackResult(String redirectUri, String cookieName, String cookieValue) {
        this.redirectUri = redirectUri;
        this.cookieName = cookieName;
        this.cookieValue = cookieValue;
    }
}
