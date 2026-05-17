package com.hogu.am_i_hogu.domain.auth.dto.request;

import lombok.Getter;

@Getter
public class OnboardingRequest {
    private final String nickname;

    public OnboardingRequest(String nickname) {
        this.nickname = nickname;
    }
}
