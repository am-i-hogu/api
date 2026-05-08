package com.hogu.am_i_hogu.domain.oauth.controller;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthCallbackResult;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import com.hogu.am_i_hogu.domain.oauth.service.UserDeletionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class OAuthController {

    private final OAuthService oauthService;
    private final UserDeletionService userDeletionService;
    private final boolean cookieSecure;
    private final String loginFailureUri;

    public OAuthController(
            OAuthService oauthService,
            UserDeletionService userDeletionService,
            @Value("${app.cookie.secure}") boolean cookieSecure,
            @Value("${app.redirect.login-failure-uri}") String loginFailureUri
    ) {
        this.oauthService = oauthService;
        this.userDeletionService = userDeletionService;
        this.cookieSecure = cookieSecure;
        this.loginFailureUri = loginFailureUri;
    }

    /**
     * [AUTH-001] 소셜 로그인 연동 페이지로 redirect
     *
     * @param provider path variable으로 전달받은 소셜 로그인 provider
     * @return '302 Found + Location 헤더의 Authorization URL' 반환
     */
    @GetMapping("/api/auth/login/{provider}")
    public ResponseEntity<Void> redirectToProviderLogin(@PathVariable String provider) {
        OAuthProvider oauthProvider = OAuthProvider.from(provider);

        String authorizationUrl = oauthService.getAuthorizationUrl(oauthProvider);

        return ResponseEntity.status(302)
                .location(URI.create(authorizationUrl))
                .build();
    }

    /**
     * [AUTH-002] 소셜 로그인 callback 처리
     *
     * @param provider  path variable로 전달받은 소셜 로그인 provider
     * @param code      소셜 서버로부터 전달받은 authorization code
     * @param state     로그인 시작 시 전달했던 state 값
     * @return '302 Found + Location 헤더 + Set-Cookie 헤더' 반환
     */
    @GetMapping("/api/auth/callback/{provider}")
    public ResponseEntity<Void> handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            OAuthProvider oauthProvider = OAuthProvider.from(provider);
            OAuthCallbackResult result = oauthService.handleCallback(oauthProvider, code, state);

            ResponseCookie cookie = ResponseCookie.from(result.getCookieName(), result.getCookieValue())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .build();

            return ResponseEntity.status(302)
                    .location(URI.create(result.getRedirectUri()))
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .build();
        } catch (CustomException e) {
            String failureRedirectUri = buildFailureRedirectUri(e.getErrorCode().getCode());

            return ResponseEntity.status(302)
                    .location(URI.create(failureRedirectUri))
                    .build();
        }
    }

    private String buildFailureRedirectUri(String errorCode) {
        return loginFailureUri + "&code=" + errorCode;
    }

    @DeleteMapping("/api/users/me")
    public ResponseEntity<Void> deleteUser(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        userDeletionService.deleteUser(userId);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
