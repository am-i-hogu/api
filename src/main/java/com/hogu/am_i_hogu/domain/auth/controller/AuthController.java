package com.hogu.am_i_hogu.domain.auth.controller;

import com.hogu.am_i_hogu.domain.auth.dto.request.OnboardingRequest;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResponse;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResult;
import com.hogu.am_i_hogu.domain.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${app.cookie.secure}") boolean cookieSecure
    ) {
        this.authService = authService;
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
    public ResponseEntity<OnboardingResponse> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody OnboardingRequest requestBody
    ) {
        String nickname = requestBody.getNickname();
        OnboardingResult result = authService.createUser(authorizationHeader, nickname);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .build();

        OnboardingResponse response = new OnboardingResponse(result.getAccessToken());

        return ResponseEntity.status(200)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
