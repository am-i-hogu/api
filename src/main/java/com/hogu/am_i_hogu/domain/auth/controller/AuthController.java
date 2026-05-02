package com.hogu.am_i_hogu.domain.auth.controller;

import com.hogu.am_i_hogu.domain.auth.dto.request.OnboardingRequest;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResponse;
import com.hogu.am_i_hogu.domain.auth.dto.response.ReissueResponse;
import com.hogu.am_i_hogu.domain.auth.dto.response.TokenPair;
import com.hogu.am_i_hogu.domain.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        TokenPair result = authService.createUser(registerToken, nickname);
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

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<ReissueResponse> reissueToken(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        TokenPair result = authService.reissueToken(authorizationHeader);
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
