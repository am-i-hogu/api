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

    @PatchMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody(required = false) UpdateProfileRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        UpdateProfileResponse response = profileUpdateService.updateProfile(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<CheckNicknameResponse> checkNickname(
            @RequestParam(name="nickname") String nickname
    ) {
        CheckNicknameResponse response = nicknameCheckService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }
}
