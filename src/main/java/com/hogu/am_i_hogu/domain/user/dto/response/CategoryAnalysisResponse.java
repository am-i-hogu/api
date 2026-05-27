package com.hogu.am_i_hogu.domain.user.dto.response;

public record CategoryAnalysisResponse(
        String category,
        Integer hoguIndex,
        String hoguLevel
) {
}
