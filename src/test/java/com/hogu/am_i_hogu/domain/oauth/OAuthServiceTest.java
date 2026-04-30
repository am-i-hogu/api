package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.oauth.config.GoogleOAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.oauth.domain.RegisterSession;
import com.hogu.am_i_hogu.domain.oauth.domain.SocialAccount;
import com.hogu.am_i_hogu.domain.oauth.dto.OAuthUserInfo;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OAuthCallbackResult;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.repository.OAuthLoginStateRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthCallbackHandler;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthCallbackHandlerFactory;
import com.hogu.am_i_hogu.domain.oauth.service.OAuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OAuthServiceTest {

    private final GoogleOAuthProperties googleOAuthProperties = mock(GoogleOAuthProperties.class);
    private final OAuthCallbackHandlerFactory oauthCallbackHandlerFactory = mock(OAuthCallbackHandlerFactory.class);
    private final OAuthCallbackHandler oauthCallbackHandler = mock(OAuthCallbackHandler.class);
    private final OAuthLoginStateRepository oauthLoginStateRepository = mock(OAuthLoginStateRepository.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final RegisterSessionRepository registerSessionRepository = mock(RegisterSessionRepository.class);
    private final OAuthService oauthService =
            new OAuthService(
                    googleOAuthProperties,
                    "http://localhost:3000/onboarding",
                    "http://localhost:3000/oauth/callback?status=LOGIN_SUCCESS",
                    oauthCallbackHandlerFactory,
                    oauthLoginStateRepository,
                    tsidGenerator,
                    socialAccountRepository,
                    jwtProvider,
                    refreshTokenRepository,
                    registerSessionRepository
            );

    /**
     * google authorization URL 생성 테스트:
     * - google OAuth 설정값과 tsid 값을 준비하고,
     * - (1) OAuthLoginState가 저장되는지 확인
     * - (2) tsidGenerator를 사용해 id를 생성하는지 확인
     * - (3) 저장된 OAuthLoginState의 id가 생성한 tsid와 같은지 확인
     * - (4) authorization URL의 고정 query parameter가 올바른지 확인
     * - (5) authorization URL의 state, nonce가 저장된 값과 같은지 확인
     */
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

    /**
     * 유효하지 않은 state 값 테스트:
     * callback 처리 시 state 값이 DB에 존재하지 않으면
     * INVALID_STATE 예외가 발생하는지 확인
     */
    @Test
    void invalidStateTest() {
        when(oauthLoginStateRepository.findByState("invalid-state-value"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(()->
                oauthService.handleCallback(
                        OAuthProvider.GOOGLE,
                        "test-auth-code",
                        "invalid-state-value"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OAuthErrorCode.INVALID_STATE));
    }

    /**
     * 이미 사용된 state 값 테스트:
     * callback 처리 시 이미 사용된 state 값이면
     * STATE_REUSED 예외가 발생하는지 확인
     */
    @Test
    void usedStateTest() {
        OAuthLoginState oauthLoginState = new OAuthLoginState(
                1L,
                OAuthProvider.GOOGLE,
                "used-state-value",
                "test-nonce-value",
                LocalDateTime.now()
        );
        oauthLoginState.markConsumed(LocalDateTime.now());

        when(oauthLoginStateRepository.findByState("used-state-value"))
                .thenReturn(Optional.of(oauthLoginState));

        assertThatThrownBy(()->
                oauthService.handleCallback(
                        OAuthProvider.GOOGLE,
                        "test-auth-code",
                        "used-state-value"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OAuthErrorCode.STATE_REUSED));
    }

    /**
     * 만료된 state 값 테스트:
     * callback 처리 시 만료된 state 값이면
     * STATE_EXPIRED 예외가 발생하는지 확인
     */
    @Test
    void expiredStateTest() {
        OAuthLoginState oauthLoginState = new OAuthLoginState(
                1L,
                OAuthProvider.GOOGLE,
                "expired-state-value",
                "test-nonce-value",
                LocalDateTime.now().minusHours(1)
        );
        when(oauthLoginStateRepository.findByState("expired-state-value"))
                .thenReturn(Optional.of(oauthLoginState));

        assertThatThrownBy(()->
                oauthService.handleCallback(
                        OAuthProvider.GOOGLE,
                        "test-auth-code",
                        "expired-state-value"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OAuthErrorCode.STATE_EXPIRED));
    }

    /**
     * callback handler 처리 실패 테스트:
     * callback 처리 시 handler 처리에 실패하면
     * INVALID_ID_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void invalidIdTokenTest() {
        OAuthLoginState oauthLoginState = new OAuthLoginState(
                1L,
                OAuthProvider.GOOGLE,
                "valid-state-value",
                "test-nonce-value",
                LocalDateTime.now()
        );
        when(oauthLoginStateRepository.findByState("valid-state-value"))
                .thenReturn(Optional.of(oauthLoginState));
        when(oauthCallbackHandlerFactory.get(OAuthProvider.GOOGLE))
                .thenReturn(oauthCallbackHandler);
        when(oauthCallbackHandler.handle("test-auth-code", oauthLoginState))
                .thenThrow(new CustomException(OAuthErrorCode.INVALID_ID_TOKEN));

        assertThatThrownBy(() ->
                oauthService.handleCallback(
                        OAuthProvider.GOOGLE,
                        "test-auth-code",
                        "valid-state-value"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OAuthErrorCode.INVALID_ID_TOKEN));
    }

    /**
     * 기존 회원 callback 처리 성공 테스트:
     * - 유효한 state 값과 기존 회원 socialAccount를 준비하고,
     * - (1) provider에 맞는 callback handler를 조회하는지 확인
     * - (2) callback handler로 provider별 callback 처리를 위임하는지 확인
     * - (3) refresh token을 발급하고 저장하는지 확인
     * - (4) OAuthLoginState를 사용 처리한 뒤 저장하는지 확인
     * - (5) 로그인 성공용 redirect 정보와 cookie 정보를 반환하는지 확인
     */
    @Test
    void handleCallbackSuccessTest() {
        OAuthLoginState oauthLoginState = new OAuthLoginState(
                1L,
                OAuthProvider.GOOGLE,
                "valid-state-value",
                "test-nonce-value",
                LocalDateTime.now()
        );
        when(oauthLoginStateRepository.findByState("valid-state-value"))
                .thenReturn(Optional.of(oauthLoginState));
        when(oauthCallbackHandlerFactory.get(OAuthProvider.GOOGLE))
                .thenReturn(oauthCallbackHandler);
        when(oauthCallbackHandler.handle("test-auth-code", oauthLoginState))
                .thenReturn(new OAuthUserInfo(OAuthProvider.GOOGLE, "google-user-id"));
        when(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-user-id"))
                .thenReturn(Optional.of(new SocialAccount(
                        1L,
                        10L,
                        OAuthProvider.GOOGLE,
                        "google-user-id",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )));
        when(jwtProvider.createRefreshToken(10L))
                .thenReturn("test-refresh-token");

        OAuthCallbackResult result = oauthService.handleCallback(
                OAuthProvider.GOOGLE,
                "test-auth-code",
                "valid-state-value"
        );

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        assertThat(oauthLoginState.isConsumed()).isTrue();
        assertThat(result.getRedirectUri()).isEqualTo("http://localhost:3000/oauth/callback?status=LOGIN_SUCCESS");
        assertThat(result.getCookieName()).isEqualTo("refreshToken");
        assertThat(result.getCookieValue()).isEqualTo("test-refresh-token");
        verify(oauthCallbackHandlerFactory).get(OAuthProvider.GOOGLE);
        verify(oauthCallbackHandler).handle("test-auth-code", oauthLoginState);
        verify(refreshTokenRepository).save(captor.capture());
        verify(oauthLoginStateRepository).save(oauthLoginState);
        assertThat(captor.getValue().getUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo("test-refresh-token");
    }

    /**
     * 신규 회원 callback 처리 성공 테스트:
     * - 유효한 state 값과 신규 회원 socialAccount 생성을 준비하고,
     * - (1) provider에 맞는 callback handler를 조회하는지 확인
     * - (2) callback handler로 provider별 callback 처리를 위임하는지 확인
     * - (3) 신규 socialAccount와 register session을 저장하는지 확인
     * - (4) OAuthLoginState를 사용 처리한 뒤 저장하는지 확인
     * - (5) 온보딩용 redirect 정보와 cookie 정보를 반환하는지 확인
     */
    @Test
    void handleCallbackNewUserTest() {
        OAuthLoginState oauthLoginState = new OAuthLoginState(
                1L,
                OAuthProvider.GOOGLE,
                "valid-state-value",
                "test-nonce-value",
                LocalDateTime.now()
        );
        when(oauthLoginStateRepository.findByState("valid-state-value"))
                .thenReturn(Optional.of(oauthLoginState));
        when(oauthCallbackHandlerFactory.get(OAuthProvider.GOOGLE))
                .thenReturn(oauthCallbackHandler);
        when(oauthCallbackHandler.handle("test-auth-code", oauthLoginState))
                .thenReturn(new OAuthUserInfo(OAuthProvider.GOOGLE, "new-google-user-id"));
        when(socialAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "new-google-user-id"))
                .thenReturn(Optional.empty());
        when(socialAccountRepository.save(any(SocialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tsidGenerator.nextId())
                .thenReturn(100L, 200L);
        when(jwtProvider.createRegisterToken(100L))
                .thenReturn("test-register-token");

        OAuthCallbackResult result = oauthService.handleCallback(
                OAuthProvider.GOOGLE,
                "test-auth-code",
                "valid-state-value"
        );

        ArgumentCaptor<SocialAccount> socialAccountCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        ArgumentCaptor<RegisterSession> registerSessionCaptor = ArgumentCaptor.forClass(RegisterSession.class);

        assertThat(oauthLoginState.isConsumed()).isTrue();
        assertThat(result.getRedirectUri()).isEqualTo("http://localhost:3000/onboarding");
        assertThat(result.getCookieName()).isEqualTo("registerToken");
        assertThat(result.getCookieValue()).isEqualTo("test-register-token");
        verify(oauthCallbackHandlerFactory).get(OAuthProvider.GOOGLE);
        verify(oauthCallbackHandler).handle("test-auth-code", oauthLoginState);
        verify(socialAccountRepository).save(socialAccountCaptor.capture());
        verify(registerSessionRepository).save(registerSessionCaptor.capture());
        verify(oauthLoginStateRepository).save(oauthLoginState);
        assertThat(socialAccountCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(socialAccountCaptor.getValue().getProviderUserId()).isEqualTo("new-google-user-id");
        assertThat(registerSessionCaptor.getValue().getSocialAccountId()).isEqualTo(100L);
        assertThat(registerSessionCaptor.getValue().getRegisterTokenHash()).isNotEqualTo("test-register-token");
    }
}
