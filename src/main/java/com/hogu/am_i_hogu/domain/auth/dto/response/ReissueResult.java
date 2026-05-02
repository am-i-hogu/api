package com.hogu.am_i_hogu.domain.auth.dto.response;

public class ReissueResult {
    private final String accessToken;
    private final String refreshToken;

    public ReissueResult(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
