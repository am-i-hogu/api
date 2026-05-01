package com.hogu.am_i_hogu.domain.user.dto.request;

import lombok.Getter;

@Getter
public class CreateUserRequest {
    private final String nickname;

    public CreateUserRequest(String nickname) {
        this.nickname = nickname;
    }
}
