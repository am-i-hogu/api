package com.hogu.am_i_hogu.domain.post.dto.request;

public record HomePostSearchRequest(
        String keyword,
        String categories,
        String sortBy,
        Integer pageSize,
        String cursor
) {
}
