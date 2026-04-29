package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OAuthLoginStateTest {

    @Test
    void createOAuthLoginStateTest() {
        String state = "this-is-state-value";
        String nonce = "this-is-nonce-value";
        LocalDateTime createdAt = LocalDateTime.now();
        OAuthLoginState oauthLoginState =
                new OAuthLoginState(1L,
                        OAuthProvider.GOOGLE,
                        state,
                        nonce,
                        createdAt);

        assertThat(oauthLoginState.getId()).isEqualTo(1L);
        assertThat(oauthLoginState.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(oauthLoginState.getState()).isEqualTo(state);
        assertThat(oauthLoginState.getNonce()).isEqualTo(nonce);
        assertThat(oauthLoginState.getCreatedAt()).isEqualTo(createdAt);
        assertThat(oauthLoginState.getExpiresAt()).isEqualTo(createdAt.plusMinutes(5));
        assertThat(oauthLoginState.getConsumedAt()).isNull();
    }
}
