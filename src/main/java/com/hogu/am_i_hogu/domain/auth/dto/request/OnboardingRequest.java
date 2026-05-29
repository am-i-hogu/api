package com.hogu.am_i_hogu.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "OnboardingRequest", description = "온보딩 요청", requiredProperties = {"nickname"})
public class OnboardingRequest {
    @Schema(
            description = "사용자 닉네임. 한글, 영문, 숫자 2자~20자이며 공백 및 특수문자를 포함할 수 없다.",
            minLength = 2,
            maxLength = 20
    )
    private final String nickname;

    public OnboardingRequest(String nickname) {
        this.nickname = nickname;
    }
}
