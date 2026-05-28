package com.hogu.am_i_hogu.domain.post.controller;

import com.hogu.am_i_hogu.domain.post.dto.request.HomePostSearchRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostCreateRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostUpdateRequest;
import com.hogu.am_i_hogu.domain.post.dto.request.PostVoteRequest;
import com.hogu.am_i_hogu.domain.post.dto.response.HomePostListResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostBookmarkResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostCreateResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostDetailResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostUpdateResponse;
import com.hogu.am_i_hogu.domain.post.dto.response.PostVoteResponse;
import com.hogu.am_i_hogu.domain.post.service.HomePostQueryService;
import com.hogu.am_i_hogu.domain.post.service.PostBookmarkService;
import com.hogu.am_i_hogu.domain.post.service.PostCreateService;
import com.hogu.am_i_hogu.domain.post.service.PostDeleteService;
import com.hogu.am_i_hogu.domain.post.service.PostDetailService;
import com.hogu.am_i_hogu.domain.post.service.PostUpdateService;
import com.hogu.am_i_hogu.domain.post.service.PostVoteService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController implements PostApiDoc {
    private final PostCreateService postCreateService;
    private final PostDetailService postDetailService;
    private final PostUpdateService postUpdateService;
    private final PostDeleteService postDeleteService;
    private final PostBookmarkService postBookmarkService;
    private final PostVoteService postVoteService;
    private final HomePostQueryService homePostQueryService;

    @GetMapping
    public ResponseEntity<HomePostListResponse> getHomePosts(
            Authentication authentication,
            @ParameterObject @ModelAttribute HomePostSearchRequest request
    ) {
        Long viewerUserId = authentication == null ? null : Long.valueOf(authentication.getName());
        HomePostListResponse response = homePostQueryService.getHomePosts(viewerUserId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostById(@PathVariable Long postId, Authentication authentication) {
        Long viewerUserId = authentication == null ? null : Long.valueOf(authentication.getName());
        PostDetailResponse response = postDetailService.getDetail(postId, viewerUserId);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PostCreateResponse> createPost(
            Authentication authentication,
            @RequestBody(required = false) PostCreateRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostCreateResponse response = postCreateService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostUpdateResponse> updatePost(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestBody(required = false) PostUpdateRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostUpdateResponse response = postUpdateService.update(postId, userId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        postDeleteService.delete(postId, userId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/bookmarks")
    public ResponseEntity<PostBookmarkResponse> createBookmark(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostBookmarkResponse response = postBookmarkService.create(userId, postId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/bookmarks")
    public ResponseEntity<PostBookmarkResponse> deleteBookmark(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostBookmarkResponse response = postBookmarkService.delete(userId, postId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/votes")
    public ResponseEntity<PostVoteResponse> vote(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestBody(required = false) PostVoteRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostVoteResponse response = postVoteService.vote(userId, postId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/votes")
    public ResponseEntity<PostVoteResponse> cancelVote(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        PostVoteResponse response = postVoteService.cancel(userId, postId);

        return ResponseEntity.ok(response);
    }
}
