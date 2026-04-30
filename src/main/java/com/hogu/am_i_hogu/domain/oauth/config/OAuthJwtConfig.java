package com.hogu.am_i_hogu.domain.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class OAuthJwtConfig {

    /**
     * Google id token 서명 검증을 위한 JwtDecoder 등록
     */
    @Bean
    public JwtDecoder googleIdTokenJwtDecoder(GoogleOAuthProperties googleOAuthProperties) {
        return NimbusJwtDecoder.withJwkSetUri(googleOAuthProperties.getJwkSetUri()).build();
    }
}
