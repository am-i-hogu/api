package com.hogu.am_i_hogu.domain.user.dto.response;

import lombok.Getter;

@Getter
public class CreateUserResponse {
    private final String accessToken;

    public CreateUserResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
