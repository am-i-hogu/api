package com.hogu.am_i_hogu.domain.oauth.dto.response;

public class OnboardingResult {
    private final String accessToken;
    private final String refreshToken;

    public OnboardingResult(
            String accessToken,
            String refreshToken
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
