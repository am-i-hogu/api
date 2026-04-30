package com.hogu.am_i_hogu.domain.oauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml에 정의된 google OAuth 설정값을 바인딩하는 설정 클래스
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "oauth.google")
public class GoogleOAuthProperties {
    private String clientId;            // google 측에서 발급받은 client id
    private String clientSecret;        // google 측에서 발급받은 client secret
    private String redirectUri;         // google 로그인 인증 완료 후 인가 코드를 전달받을 callback URL
    private String authorizationUri;    // google 로그인 페이지로 이동시키기 위한 google 인증 서버의 endpoint URL
    private String tokenUri;            // token 교환 endpoint
    private String scope;               // 유저에게 권한을 요구할 정보의 범위
}
