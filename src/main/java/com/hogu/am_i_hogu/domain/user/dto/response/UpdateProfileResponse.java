package com.hogu.am_i_hogu.domain.user.dto.response;

public record UpdateProfileResponse (
        Long id,
        String nickname,
        String profileImageUrl
) {
}
