package com.hogu.am_i_hogu.domain.auth.controller;

import com.hogu.am_i_hogu.domain.auth.dto.request.OnboardingRequest;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResponse;
import com.hogu.am_i_hogu.domain.auth.dto.response.ReissueResponse;
import com.hogu.am_i_hogu.domain.auth.dto.response.TokenPair;
import com.hogu.am_i_hogu.domain.auth.service.OnboardingService;
import com.hogu.am_i_hogu.domain.auth.service.ReissueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final OnboardingService onboardingService;
    private final ReissueService reissueService;
    private final boolean cookieSecure;

    public AuthController(
            OnboardingService onboardingService,
            ReissueService reissueService,
            @Value("${app.cookie.secure}") boolean cookieSecure
    ) {
        this.onboardingService = onboardingService;
        this.reissueService = reissueService;
        this.cookieSecure = cookieSecure;
    }

    /**
     * [ONBOARDING-001] 온보딩
     *
     * @param registerToken 요청으로 들어온 register token
     * @param requestBody 요청 본문
     * @return refresh token, access token
     */
    @PostMapping("/api/users")
    public ResponseEntity<OnboardingResponse> createUser(
            @CookieValue(name = "registerToken", required = false) String registerToken,
            @RequestBody OnboardingRequest requestBody
    ) {
        String nickname = requestBody.getNickname();
        TokenPair result = onboardingService.createUser(registerToken, nickname);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .build();

        ResponseCookie deleteRegisterTokenCookie = ResponseCookie.from("registerToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .build();

        OnboardingResponse response = new OnboardingResponse(result.getAccessToken());

        return ResponseEntity.status(200)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deleteRegisterTokenCookie.toString())
                .body(response);
    }

    /**
     * [AUTH-003] 토큰 재발급
     *
     * @param refreshToken  access token 재발급을 위한 refresh token
     * @return  재발급된 access token, refresh token
     */
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<ReissueResponse> reissueToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        TokenPair result = reissueService.reissueToken(refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .build();

        ReissueResponse response = new ReissueResponse(result.getAccessToken());

        return ResponseEntity.status(200)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
