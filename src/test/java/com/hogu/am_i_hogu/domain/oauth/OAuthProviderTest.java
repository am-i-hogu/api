package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class OAuthProviderTest {

    @Test
    void fromSupportedProviderTest() {
        OAuthProvider provider = OAuthProvider.from("GOOGLE");

        assertThat(provider).isEqualTo(OAuthProvider.GOOGLE);
    }

    @Test
    void fromUnsupportedProviderTest() {
        assertThatThrownBy(()->OAuthProvider.from("INVALID_PROVIDER"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(OAuthErrorCode.UNSUPPORTED_PROVIDER);
                });
    }
}
