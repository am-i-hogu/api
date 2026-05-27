package com.hogu.am_i_hogu.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record HomePostListResponse(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long totalPostCount,
        List<HomePostItemResponse> posts,
        boolean hasNext,
        String nextCursor
) {
}
