package com.hogu.am_i_hogu.domain.user.dto.request;

public record CursorRequest(
        Integer pageSize,
        String cursor
) {
}
