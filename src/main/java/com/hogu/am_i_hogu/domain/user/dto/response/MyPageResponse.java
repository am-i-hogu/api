package com.hogu.am_i_hogu.domain.user.dto.response;

public record MyPageResponse(
        String nickname,
        String profileImageUrl,
        Integer hoguIndex,
        String hoguLevel,
        String hoguDescription
) {
}
