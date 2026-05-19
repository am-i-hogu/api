package com.hogu.am_i_hogu.domain.comment.dto.response;

import java.util.List;

public record CommentReadResponse(
        List<CommentItemResponse> comments,
        boolean hasNext,
        String nextCursor
) {
}
