package com.hogu.am_i_hogu.domain.user.dto.response;

public record MyVotePostResponse(
        Long postId,
        String title,
        String category,
        long commentCount,
        boolean isDeleted
) {
}
