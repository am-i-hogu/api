package com.hogu.am_i_hogu.common.pagination;

public record CursorRequest(
        Integer pageSize,
        String cursor
) {
}
