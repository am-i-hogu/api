package com.hogu.am_i_hogu.domain.user.dto.response;

import java.util.List;

public record MyBookmarkListResponse(
        List<MyPostItemResponse> posts,
        boolean hasNext,
        String nextCursor
) {
}
