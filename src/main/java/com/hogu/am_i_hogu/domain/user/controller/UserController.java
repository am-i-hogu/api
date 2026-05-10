package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.domain.user.dto.request.UpdateProfileRequest;
import com.hogu.am_i_hogu.domain.user.dto.response.CheckNicknameResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.UpdateProfileResponse;
import com.hogu.am_i_hogu.domain.user.service.NicknameCheckService;
import com.hogu.am_i_hogu.domain.user.service.ProfileUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final NicknameCheckService nicknameCheckService;
    private final ProfileUpdateService profileUpdateService;

    public UserController(
            NicknameCheckService nicknameCheckService,
            ProfileUpdateService profileUpdateService
    ) {
        this.nicknameCheckService = nicknameCheckService;
        this.profileUpdateService = profileUpdateService;
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
}
