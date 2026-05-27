package com.hogu.am_i_hogu.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "ReissueResponse", description = "토큰 재발급 응답")
public class ReissueResponse {
    @Schema(
            description = "재발급된 access token",
            example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    private final String accessToken;

    public ReissueResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
