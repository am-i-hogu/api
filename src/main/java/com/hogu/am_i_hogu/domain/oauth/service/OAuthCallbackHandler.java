package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;

public interface OAuthCallbackHandler {

    OAuthProvider supports();
    OAuthUserInfo handle(String code, OAuthLoginState oauthLoginState);
}
