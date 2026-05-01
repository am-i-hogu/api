package com.hogu.am_i_hogu.domain.user.controller;

import com.hogu.am_i_hogu.domain.user.dto.request.CreateUserRequest;
import com.hogu.am_i_hogu.domain.user.dto.response.CreateUserResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.OnboardingResult;
import com.hogu.am_i_hogu.domain.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;
    private final boolean cookieSecure;

    public UserController(
            UserService userService,
            @Value("${app.cookie.secure}") boolean cookieSecure
    ) {
        this.userService = userService;
        this.cookieSecure = cookieSecure;
    }

    /**
     * [ONBOARDING-001] 온보딩
     *
     * @param authorizationHeader 요청으로 들어온 헤더
     * @param requestBody         요청 본문
     * @return refresh token, access token
     */
    @PostMapping("/api/users")
    public ResponseEntity<CreateUserResponse> createUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody CreateUserRequest requestBody
    ) {
        String nickname = requestBody.getNickname();
        OnboardingResult result = userService.createUser(authorizationHeader, nickname);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .build();

        CreateUserResponse response = new CreateUserResponse(result.getAccessToken());

        return ResponseEntity.status(200)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
