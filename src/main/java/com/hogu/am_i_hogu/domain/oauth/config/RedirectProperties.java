package com.hogu.am_i_hogu.domain.oauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth callback 이후 프론트 redirect 주소 설정값 바인딩하는 클래스
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.redirect")
public class RedirectProperties {
    private String onboardingUri;
    private String loginSuccessUri;
}
