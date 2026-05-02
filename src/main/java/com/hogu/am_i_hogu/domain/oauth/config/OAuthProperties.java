package com.hogu.am_i_hogu.domain.oauth.config;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private OAuthClientProperties google;
    private OAuthClientProperties kakao;

    public OAuthClientProperties getClientProperties(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> google;
            case KAKAO -> kakao;
        };
    }
}
