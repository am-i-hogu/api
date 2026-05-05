package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.domain.user.dto.response.CheckNicknameResponse;
import com.hogu.am_i_hogu.domain.user.service.NicknameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final NicknameService nicknameService;

    public UserController(NicknameService nicknameService) {
        this.nicknameService = nicknameService;
    }

    @GetMapping("/api/users/check-nickname")
    public ResponseEntity<CheckNicknameResponse> checkNickname(
            @RequestParam(name="nickname") String nickname
    ) {
        CheckNicknameResponse response = nicknameService.checkNickname(nickname);
        return ResponseEntity.status(200)
                .body(response);
    }
}
