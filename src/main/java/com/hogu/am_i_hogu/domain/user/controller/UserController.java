package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.common.pagination.CursorRequest;
import com.hogu.am_i_hogu.domain.user.dto.request.UpdateProfileRequest;
import com.hogu.am_i_hogu.domain.user.dto.response.*;
import com.hogu.am_i_hogu.domain.user.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final NicknameCheckService nicknameCheckService;
    private final ProfileUpdateService profileUpdateService;
    private final MyPostQueryService myPostQueryService;
    private final MyCommentQueryService myCommentQueryService;
    private final MyBookmarkQueryService myBookmarkQueryService;

    public UserController(
            NicknameCheckService nicknameCheckService,
            ProfileUpdateService profileUpdateService,
            MyPostQueryService myPostQueryService,
            MyCommentQueryService myCommentQueryService,
            MyBookmarkQueryService myBookmarkQueryService) {
        this.nicknameCheckService = nicknameCheckService;
        this.profileUpdateService = profileUpdateService;
        this.myPostQueryService = myPostQueryService;
        this.myCommentQueryService = myCommentQueryService;
        this.myBookmarkQueryService = myBookmarkQueryService;
    }

    /**
     * [USER-001] 유저 프로필 수정
     *
     * @param authentication    유저 인증 정보
     * @param request           프로필 수정 사항
     * @return 수정 사항이 반영된 프로필 정보
     */
    @PatchMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody(required = false) UpdateProfileRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        UpdateProfileResponse response = profileUpdateService.updateProfile(userId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * [USER-002] 닉네임 중복 확인
     *
     * @param nickname  중복 확인할 닉네임
     * @return 닉네임 사용 가능 여부
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<CheckNicknameResponse> checkNickname(
            @RequestParam(name="nickname", required = false) String nickname
    ) {
        CheckNicknameResponse response = nicknameCheckService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }

    /**
     * [HISTORY-001] 작성한 게시물 리스트 조회
     *
     * @param authentication    유저 인증 정보
     * @param cursorRequest     cursor 정보(post 생성 일시, post id 포함)
     * @return 작성한 게시물 리스트
     */
    @GetMapping("/me/posts")
    public ResponseEntity<MyPostListResponse> getMyPosts(
            Authentication authentication,
            @ModelAttribute CursorRequest cursorRequest
    ) {
        Long userId = Long.valueOf(authentication.getName());
        MyPostListResponse response = myPostQueryService.getMyPosts(userId, cursorRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * [HISTORY-002] 작성한 댓글 리스트 조회
     *
     * @param authentication    유저 인증 정보
     * @param cursorRequest     cursor 정보(comment 생성 일시, comment id 포함)
     * @return 조회된 댓글 리스트
     */
    @GetMapping("/me/comments")
    public ResponseEntity<MyCommentListResponse> getMyComments(
            Authentication authentication,
            @ModelAttribute CursorRequest cursorRequest
    ) {
        Long userId = Long.valueOf(authentication.getName());
        MyCommentListResponse response = myCommentQueryService.getMyComments(userId, cursorRequest);

        return ResponseEntity.ok(response);
    }

    /**
     * [HISTORY-003] 북마크한 게시물 조회
     *
     * @param authentication    유저 인증 정보
     * @param cursorRequest     cursor 정보(bookmark 생성 일시, post id 포함)
     * @return 조회된 게시물 리스트
     */
    @GetMapping("/me/bookmarks")
    public ResponseEntity<MyBookmarkListResponse> getMyBookmarks(
            Authentication authentication,
            @ModelAttribute CursorRequest cursorRequest
    ) {
        Long userId = Long.valueOf(authentication.getName());
        MyBookmarkListResponse response = myBookmarkQueryService.getMyBookmarks(userId, cursorRequest);

        return ResponseEntity.ok(response);
    }
}
