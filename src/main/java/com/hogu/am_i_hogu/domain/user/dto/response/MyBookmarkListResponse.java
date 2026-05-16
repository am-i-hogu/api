package com.hogu.am_i_hogu.domain.user.dto.response;

import java.util.List;

public record MyBookmarkListResponse(
        List<MyBookmarkItemResponse> posts,
        boolean hasNext,
        String nextCursor
) {
}
