package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OAuthLoginStateTest {

    /**
     * OAuthLoginState 생성 테스트:
     * - id, provider, state, nonce, createdAt 값을 주어 객체를 생성하고,
     * - (1) 각 필드가 전달한 값과 같은지 확인
     * - (2) expiresAt이 createdAt 기준 5분 뒤로 설정되는지 확인
     * - (3) consumedAt이 null로 초기화되는지 확인
     */
    @Test
    void createOAuthLoginStateReturnsInitializedStateWhenArgumentsAreValid() {
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

    /**
     * 만료된 state 검증 테스트:
     * id, provider, state, nonce, createdAt 값을 주어 1시간 전에 만들어진 객체를 생성하고,
     * isExpired()가 true를 리턴하는지 확인
     */
    @Test
    void isExpiredReturnsTrueWhenStateIsExpired() {
        String state = "this-is-state-value";
        String nonce = "this-is-nonce-value";
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        OAuthLoginState oauthLoginState =
                new OAuthLoginState(1L,
                        OAuthProvider.GOOGLE,
                        state,
                        nonce,
                        createdAt);

        assertThat(oauthLoginState.isExpired(LocalDateTime.now())).isTrue();
    }

    /**
     * 사용된 state 검증 테스트:
     * - id, provider, state, nonce, createdAt 값을 주어 객체를 생성하고,
     * - (1) 생성 직후 isConsumed()가 false를 반환하는지 확인
     * - (2) 사용 직후 isConsumed()가 true를 반환하는지 확인
     */
    @Test
    void isConsumedReturnsTrueWhenStateIsConsumed() {
        String state = "this-is-state-value";
        String nonce = "this-is-nonce-value";
        LocalDateTime createdAt = LocalDateTime.now();
        OAuthLoginState oauthLoginState =
                new OAuthLoginState(1L,
                        OAuthProvider.GOOGLE,
                        state,
                        nonce,
                        createdAt);

        assertThat(oauthLoginState.isConsumed()).isFalse();

        LocalDateTime consumedAt = LocalDateTime.now();
        oauthLoginState.markConsumed(consumedAt);

        assertThat(oauthLoginState.isConsumed()).isTrue();
    }

    /**
     * state 사용 처리 테스트:
     * id, provider, state, nonce, createdAt 값을 주어 객체를 생성하고,
     * markConsumed() 호출 이후 consumedAt 필드 값이 사용 시간과 일치하는지 확인
     */
    @Test
    void markConsumedUpdatesConsumedAtWhenStateIsConsumed() {
        String state = "this-is-state-value";
        String nonce = "this-is-nonce-value";
        LocalDateTime createdAt = LocalDateTime.now();
        OAuthLoginState oauthLoginState =
                new OAuthLoginState(1L,
                        OAuthProvider.GOOGLE,
                        state,
                        nonce,
                        createdAt);

        LocalDateTime consumedAt = LocalDateTime.now();
        oauthLoginState.markConsumed(consumedAt);
        assertThat(oauthLoginState.getConsumedAt()).isEqualTo(consumedAt);
    }
}
