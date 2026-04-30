package com.hogu.am_i_hogu.domain.oauth.controller;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class OAuthController {

    private final OAuthService oauthService;

    public OAuthController(OAuthService oauthService) {
        this.oauthService = oauthService;
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
}
