package com.hogu.am_i_hogu.domain.oauth.dto.response;

public class CreateUserResponse {
    private final String accessToken;

    public CreateUserResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
