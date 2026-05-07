package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.domain.PostBookmark;
import com.hogu.am_i_hogu.domain.post.domain.PostBookmarkId;
import com.hogu.am_i_hogu.domain.post.dto.response.PostBookmarkResponse;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostBookmarkRepository;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostBookmarkService {

    private final PostRepository postRepository;
    private final PostBookmarkRepository postBookmarkRepository;

    @Transactional
    public PostBookmarkResponse create(Long userId, Long postId) {
        validateBookmarkablePost(postId);

        PostBookmarkId id = new PostBookmarkId(userId, postId);
        if (postBookmarkRepository.existsById(id)) {
            throw new CustomException(PostErrorCode.DUPLICATE_REQUEST);
        }

        postBookmarkRepository.save(new PostBookmark(id, LocalDateTime.now()));

        return new PostBookmarkResponse(true);
    }

    @Transactional
    public PostBookmarkResponse delete(Long userId, Long postId) {
        validateBookmarkablePost(postId);

        PostBookmarkId id = new PostBookmarkId(userId, postId);
        if (postBookmarkRepository.existsById(id)) {
            postBookmarkRepository.deleteById(id);
        }

        return new PostBookmarkResponse(false);
    }

    private void validateBookmarkablePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }
    }
}
