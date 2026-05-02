package com.hogu.am_i_hogu.domain.oauth.config;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
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
    public JwtDecoder googleIdTokenJwtDecoder(OAuthProperties oauthProperties) {
        OAuthClientProperties properties = oauthProperties.getClientProperties(OAuthProvider.GOOGLE);
        return NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
    }
}
