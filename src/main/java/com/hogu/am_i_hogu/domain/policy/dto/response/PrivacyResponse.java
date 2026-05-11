package com.hogu.am_i_hogu.domain.policy.dto.response;

import java.time.LocalDateTime;

public record PrivacyResponse(
    String version,
    LocalDateTime updatedAt,
    String content
) {
}
