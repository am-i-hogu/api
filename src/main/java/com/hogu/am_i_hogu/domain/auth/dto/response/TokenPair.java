package com.hogu.am_i_hogu.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;

    public TokenPair(
            String accessToken,
            String refreshToken
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
