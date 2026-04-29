package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.oauth.config.GoogleOAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.repository.OAuthLoginStateRepository;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class OAuthServiceTest {

    private final GoogleOAuthProperties googleOAuthProperties = mock(GoogleOAuthProperties.class);
    private final OAuthLoginStateRepository oauthLoginStateRepository = mock(OAuthLoginStateRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final OAuthService oauthService =
            new OAuthService(googleOAuthProperties, oauthLoginStateRepository, tsidGenerator);

    @Test
    void getAuthorizationUrlTest() {
        when(googleOAuthProperties.getAuthorizationUri())
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
        when(googleOAuthProperties.getClientId())
                .thenReturn("test-client-id");
        when(googleOAuthProperties.getRedirectUri())
                .thenReturn("http://localhost:8080/api/auth/callback/GOOGLE");
        when(googleOAuthProperties.getScope())
                .thenReturn("openid");
        when(tsidGenerator.nextId())
                .thenReturn(1L);

        String authorizationUrl = oauthService.getAuthorizationUrl(OAuthProvider.GOOGLE);

        ArgumentCaptor<OAuthLoginState> captor = ArgumentCaptor.forClass(OAuthLoginState.class);
        verify(oauthLoginStateRepository).save(captor.capture());
        verify(tsidGenerator).nextId();
        OAuthLoginState savedState = captor.getValue();

        Map<String, List<String>> params = UriComponentsBuilder
                .fromUriString(authorizationUrl)
                .build()
                .getQueryParams();

        assertThat(savedState.getId()).isEqualTo(1L);
        assertThat(authorizationUrl).isNotBlank();
        assertThat(params.get("client_id").get(0)).isEqualTo("test-client-id");
        assertThat(params.get("response_type").get(0)).isEqualTo("code");
        assertThat(params.get("scope").get(0)).isEqualTo("openid");
        assertThat(params.get("redirect_uri").get(0)).isEqualTo("http://localhost:8080/api/auth/callback/GOOGLE");
        assertThat(params.get("state").get(0)).isEqualTo(savedState.getState());
        assertThat(params.get("nonce").get(0)).isEqualTo(savedState.getNonce());
    }
}
