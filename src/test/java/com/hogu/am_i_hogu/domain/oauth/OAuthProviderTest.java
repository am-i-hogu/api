package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class OAuthProviderTest {

    /**
     * 유효한 provider 값 검증 테스트:
     * 지원하는 provider 이름이 주어졌을 때
     * 올바른 OAuthProvider Enum 상수로 변환되는지 테스트
     */
    @Test
    void fromReturnsProviderWhenProviderNameIsSupported() {
        OAuthProvider provider = OAuthProvider.from("GOOGLE");

        assertThat(provider).isEqualTo(OAuthProvider.GOOGLE);
    }

    /**
     * 유효하지 않은 provider 값 검증 테스트:
     * 지원하지 않는 provider 이름이 주어졌을 때
     * UNSUPPORTED_PROVIDER 예외가 발생하는지 테스트
     */
    @Test
    void fromThrowsUnsupportedProviderWhenProviderNameIsUnsupported() {
        assertThatThrownBy(()->OAuthProvider.from("INVALID_PROVIDER"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(OAuthErrorCode.UNSUPPORTED_PROVIDER);
                });
    }
}
