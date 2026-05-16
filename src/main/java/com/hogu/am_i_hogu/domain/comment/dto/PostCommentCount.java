package com.hogu.am_i_hogu.domain.comment.dto;

public record PostCommentCount(
        Long postId,
        long commentCount
) {
}
