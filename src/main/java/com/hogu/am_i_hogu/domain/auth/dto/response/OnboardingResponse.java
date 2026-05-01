package com.hogu.am_i_hogu.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class OnboardingResponse {
    private final String accessToken;

    public OnboardingResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
