package com.hogu.am_i_hogu.domain.user.dto.response;

import lombok.Getter;

@Getter
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
