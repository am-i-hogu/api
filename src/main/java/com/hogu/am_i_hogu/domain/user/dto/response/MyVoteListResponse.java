package com.hogu.am_i_hogu.domain.user.dto.response;

import java.util.List;

public record MyVoteListResponse(
        List<MyVoteItemResponse> votes,
        boolean hasNext,
        String nextCursor
) {
}
