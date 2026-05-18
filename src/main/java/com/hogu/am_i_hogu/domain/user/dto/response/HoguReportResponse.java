package com.hogu.am_i_hogu.domain.user.dto.response;

import java.util.List;

public record HoguReportResponse(
        String nickname,
        String profileImageUrl,
        Integer hoguIndex,
        String hoguLevel,
        String hoguDescription,
        List<CategoryAnalysisResponse> categoryAnalysis,
        Integer totalPostCount,
        Integer hoguPostCount,
        Integer notHoguPostCount
) {
}
