package com.hogu.am_i_hogu.domain.user.dto.response;

import java.util.List;

public record MyCommentListResponse(
        List<MyCommentItemResponse> comments,
        boolean hasNext,
        String nextCursor
) {
}
