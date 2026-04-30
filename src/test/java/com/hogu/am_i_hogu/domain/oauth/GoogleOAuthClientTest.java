package com.hogu.am_i_hogu.domain.oauth;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.config.GoogleOAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.service.GoogleOAuthClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class GoogleOAuthClientTest {
    private final GoogleOAuthProperties googleOAuthProperties = new GoogleOAuthProperties();
    private final RestClient.Builder restClientBuilder = mock(RestClient.Builder.class);
    private final RestClient restClient = mock(RestClient.class);
    private final RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    private final RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

    /**
     * google token endpoint 요청 성공 테스트:
     * - code와 google OAuth 설정값을 준비하고,
     * - (1) RestClient.Builder로 RestClient를 생성하는지 확인
     * - (2) tokenUri로 POST 요청하는지 확인
     * - (3) content type이 FORM_URLENCODED인지 확인
     * - (4) 요청 본문에 필요한 form parameter가 포함되는지 확인
     * - (5) 응답 본문이 TokenResponse로 정상 반환되는지 확인
     */
    @Test
    void requestTokenTest() {
        googleOAuthProperties.setClientId("test-client-id");
        googleOAuthProperties.setClientSecret("test-client-secret");
        googleOAuthProperties.setRedirectUri("http://localhost:8080/api/auth/callback/GOOGLE");
        googleOAuthProperties.setTokenUri("https://oauth2.googleapis.com/token");

        TokenResponse tokenResponse = mock(TokenResponse.class);

        when(restClientBuilder.build())
                .thenReturn(restClient);
        when(restClient.post())
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("https://oauth2.googleapis.com/token"))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve())
                .thenReturn(responseSpec);
        when(responseSpec.body(TokenResponse.class))
                .thenReturn(tokenResponse);

        GoogleOAuthClient googleOAuthClient =
                new GoogleOAuthClient(googleOAuthProperties, restClientBuilder);

        TokenResponse response = googleOAuthClient.requestToken("test-auth-code");

        ArgumentCaptor<MultiValueMap<String, String>> captor =
                ArgumentCaptor.forClass(MultiValueMap.class);

        verify(restClientBuilder).build();
        verify(restClient).post();
        verify(requestBodyUriSpec).uri("https://oauth2.googleapis.com/token");
        verify(requestBodyUriSpec).contentType(MediaType.APPLICATION_FORM_URLENCODED);
        verify(requestBodyUriSpec).body(captor.capture());
        verify(requestBodyUriSpec).retrieve();
        verify(responseSpec).body(TokenResponse.class);

        MultiValueMap<String, String> requestBody = captor.getValue();

        assertThat(requestBody.getFirst("code")).isEqualTo("test-auth-code");
        assertThat(requestBody.getFirst("client_id")).isEqualTo("test-client-id");
        assertThat(requestBody.getFirst("client_secret")).isEqualTo("test-client-secret");
        assertThat(requestBody.getFirst("redirect_uri")).isEqualTo("http://localhost:8080/api/auth/callback/GOOGLE");
        assertThat(requestBody.getFirst("grant_type")).isEqualTo("authorization_code");
        assertThat(response).isEqualTo(tokenResponse);
    }

    /**
     * google token endpoint 400 응답 테스트:
     * 유효하지 않은 authorization code로 token endpoint 호출 시
     * INVALID_AUTH_CODE 예외가 발생하는지 테스트
     */
    @Test
    void invalidAuthCodeTest() {
        googleOAuthProperties.setClientId("test-client-id");
        googleOAuthProperties.setClientSecret("test-client-secret");
        googleOAuthProperties.setRedirectUri("http://localhost:8080/api/auth/callback/GOOGLE");
        googleOAuthProperties.setTokenUri("https://oauth2.googleapis.com/token");

        when(restClientBuilder.build())
                .thenReturn(restClient);
        when(restClient.post())
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any(MediaType.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve())
                .thenReturn(responseSpec);
        when(responseSpec.body(TokenResponse.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ));

        GoogleOAuthClient googleOAuthClient =
                new GoogleOAuthClient(googleOAuthProperties, restClientBuilder);

        assertThatThrownBy(() -> googleOAuthClient.requestToken("invalid-auth-code"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OAuthErrorCode.INVALID_AUTH_CODE));
    }

    /**
     * google token endpoint 5xx 응답 테스트:
     * token endpoint 호출에 실패했을 때
     * SOCIAL_SERVER_ERROR 예외가 발생하는지 테스트
     */
    @Test
    void socialServerErrorTest() {
        googleOAuthProperties.setClientId("test-client-id");
        googleOAuthProperties.setClientSecret("test-client-secret");
        googleOAuthProperties.setRedirectUri("http://localhost:8080/api/auth/callback/GOOGLE");
        googleOAuthProperties.setTokenUri("https://oauth2.googleapis.com/token");

        when(restClientBuilder.build())
                .thenReturn(restClient);
        when(restClient.post())
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any(MediaType.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class)))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).
                thenReturn(responseSpec);
        when(responseSpec.body(TokenResponse.class))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ));

        GoogleOAuthClient googleOAuthClient =
                new GoogleOAuthClient(googleOAuthProperties, restClientBuilder);

        assertThatThrownBy(() -> googleOAuthClient.requestToken("test-auth-code"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(OAuthErrorCode.SOCIAL_SERVER_ERROR));
    }
}
