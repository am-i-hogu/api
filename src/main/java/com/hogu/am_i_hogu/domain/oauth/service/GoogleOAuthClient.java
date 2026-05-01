package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.config.GoogleOAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoogleOAuthClient {
    private final GoogleOAuthProperties googleOAuthProperties;
    private final RestClient restClient;

    public GoogleOAuthClient(
            GoogleOAuthProperties googleOAuthProperties,
            RestClient.Builder restClientBuilder) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.restClient = restClientBuilder.build();
    }

    /**
     * authorization code를 이용해 token endpoint에 token 교환 요청
     * @param code  소셜 서버에서 보내준 authorization code
     * @return token endpoint로부터 받아온 token 응답 객체
     */
    public TokenResponse requestToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleOAuthProperties.getClientId());
        body.add("client_secret", googleOAuthProperties.getClientSecret());
        body.add("redirect_uri", googleOAuthProperties.getRedirectUri());
        body.add("grant_type", "authorization_code");

        try {
            return restClient.post()
                    .uri(googleOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                throw new CustomException(OAuthErrorCode.INVALID_AUTH_CODE);
            }
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }
    }

}
