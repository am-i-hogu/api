package com.hogu.am_i_hogu.domain.post.dto.request;

public record PostImageRequest(
        String imageUrl,
        Integer order,
        Boolean isThumbnail
) {
}
