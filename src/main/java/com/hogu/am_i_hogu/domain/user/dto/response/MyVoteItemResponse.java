package com.hogu.am_i_hogu.domain.user.dto.response;

import java.time.LocalDateTime;

public record MyVoteItemResponse(
        String myVote,
        LocalDateTime createdAt,
        MyVotePostResponse post
) {
}
