package com.hogu.am_i_hogu.domain.auth.dto.response;

public class ReissueResponse {
    private final String accessToken;

    public ReissueResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
