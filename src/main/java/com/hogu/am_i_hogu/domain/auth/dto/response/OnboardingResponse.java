package com.hogu.am_i_hogu.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "OnboardingResponse", description = "온보딩 응답")
public class OnboardingResponse {
    @Schema(
            description = "사용자의 access token",
            example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    private final String accessToken;

    public OnboardingResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
