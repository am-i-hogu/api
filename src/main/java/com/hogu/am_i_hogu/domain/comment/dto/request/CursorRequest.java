package com.hogu.am_i_hogu.domain.comment.dto.request;

public record CursorRequest(
        String sortBy,
        Integer pageSize,
        String cursor
) {
}
