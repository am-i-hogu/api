package com.hogu.am_i_hogu.domain.post.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.post.exception.PostErrorCode;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostDeleteService {
    private final PostRepository postRepository;

    /**
     * 게시물을 soft delete 처리한다.
     *
     * @param postId 삭제할 게시물 ID
     * @param userId 삭제를 요청한 사용자 ID
     */
    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        validateDeletable(post, userId);

        post.delete(LocalDateTime.now());
    }

    /**
     * 게시물 ID로 게시물을 조회한다.
     *
     * @param postId 조회할 게시물 ID
     * @return 조회된 게시물 엔티티
     */
    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
    }

    /**
     * 이미 삭제된 게시물인지, 요청자가 작성자인지 검증한다.
     *
     * @param post 삭제하려는 게시물
     * @param userId 삭제를 요청한 사용자 ID
     */
    private void validateDeletable(Post post, Long userId) {
        if (post.isDeleted()) {
            throw new CustomException(PostErrorCode.POST_ALREADY_DELETED);
        }

        if (!post.getWriter().getId().equals(userId)) {
            throw new CustomException(CommonErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
