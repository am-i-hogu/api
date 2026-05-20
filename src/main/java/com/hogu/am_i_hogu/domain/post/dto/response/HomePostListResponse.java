package com.hogu.am_i_hogu.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomePostListResponse(
        Long totalPostCount,
        List<HomePostItemResponse> posts,
        boolean hasNext,
        String nextCursor
) {
}
