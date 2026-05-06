package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.domain.user.dto.request.UpdateProfileRequest;
import com.hogu.am_i_hogu.domain.user.dto.response.CheckNicknameResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.UpdateProfileResponse;
import com.hogu.am_i_hogu.domain.user.service.NicknameService;
import com.hogu.am_i_hogu.domain.user.service.UpdateProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final NicknameService nicknameService;
    private final UpdateProfileService updateProfileService;

    public UserController(
            NicknameService nicknameService,
            UpdateProfileService updateProfileService
    ) {
        this.nicknameService = nicknameService;
        this.updateProfileService = updateProfileService;
    }

    @PatchMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody(required = false) UpdateProfileRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        UpdateProfileResponse response = updateProfileService.updateProfile(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<CheckNicknameResponse> checkNickname(
            @RequestParam(name="nickname") String nickname
    ) {
        CheckNicknameResponse response = nicknameService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }
}
