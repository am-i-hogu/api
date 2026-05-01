package com.hogu.am_i_hogu.domain.oauth.dto;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import lombok.Getter;

/**
 * 소셜 로그인 provider에서 조회한 사용자 식별 정보를 담는 DTO
 */
@Getter
public class OAuthUserInfo {
    private final OAuthProvider provider;
    private final String providerUserId;

    public OAuthUserInfo(OAuthProvider provider, String providerUserId) {
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}
