package com.hogu.am_i_hogu.domain.oauth.controller;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthCallbackResult;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import org.springframework.beans.factory.annotation.Value;
import com.hogu.am_i_hogu.domain.auth.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class OAuthController {

    private final OAuthService oauthService;
    private final boolean cookieSecure;

    public OAuthController(
            OAuthService oauthService,
            @Value("${app.cookie.secure}") boolean cookieSecure
    ) {
        this.oauthService = oauthService;
        this.cookieSecure = cookieSecure;
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
    }
}
