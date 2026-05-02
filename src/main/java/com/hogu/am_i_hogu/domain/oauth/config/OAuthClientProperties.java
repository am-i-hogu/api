package com.hogu.am_i_hogu.domain.oauth.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * application.yml에 정의된 OAuth 설정값을 바인딩하는 설정 클래스
 */
@Getter
@Setter
public class OAuthClientProperties {
    private String clientId;            // client id
    private String clientSecret;        // client secret
    private String redirectUri;         // 로그인 인증 완료 후 인가 코드를 전달받을 callback URL
    private String authorizationUri;    // 로그인 페이지로 이동시키기 위한 인증 서버의 endpoint URL
    private String tokenUri;            // token 교환 endpoint
    private String jwkSetUri;           // 공개키 목록 endpoint
    private List<String> issuerUris;    // token 발급 주체 정보
    private String scope;               // 유저에게 권한을 요구할 정보의 범위
}
