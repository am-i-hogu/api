package com.hogu.am_i_hogu.domain.user.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateProfileRequest (
        String nickname,
        JsonNullable<String> profileImageUrl
) {
}
