package com.hogu.am_i_hogu.domain.oauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.config.OAuthClientProperties;
import com.hogu.am_i_hogu.domain.oauth.config.OAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.dto.response.KakaoErrorResponse;
import com.hogu.am_i_hogu.domain.oauth.dto.response.TokenResponse;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OAuthClient {
    private final OAuthProperties oauthProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OAuthClient(
            OAuthProperties oauthProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.oauthProperties = oauthProperties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * authorization code를 이용해 token endpoint에 token 교환 요청
     * @param code  소셜 서버에서 보내준 authorization code
     * @return token endpoint로부터 받아온 token 응답 객체
     */
    public TokenResponse requestToken(String code, OAuthProvider provider) {
        OAuthClientProperties properties = oauthProperties.getClientProperties(provider);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("redirect_uri", properties.getRedirectUri());
        body.add("grant_type", "authorization_code");

        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
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

    public void revokeGoogleToken(String token) {
        OAuthClientProperties properties = oauthProperties.getClientProperties(OAuthProvider.GOOGLE);

        try {
            restClient.post()
                    .uri(properties.getRevokeUri() + "?token={token}", token)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_REJECTED);
            }
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }
    }

    public void unlinkKakao(String accessToken) {
        OAuthClientProperties properties = oauthProperties.getClientProperties(OAuthProvider.KAKAO);

        try {
            restClient.post()
                    .uri(properties.getRevokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                throw mapKakaoException(e);
            }
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }
    }

    private CustomException mapKakaoException(RestClientResponseException e) {
        KakaoErrorResponse errorResponse = parseKakaoErrorResponse(e.getResponseBodyAsString());

        if (errorResponse != null && errorResponse.getCode() != null) {
            if (errorResponse.getCode() == -401) {
                return new CustomException(OAuthErrorCode.SOCIAL_SERVER_REJECTED);
            }
            return new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }

        return new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
    }

    private KakaoErrorResponse parseKakaoErrorResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, KakaoErrorResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public TokenResponse reissueKakaoToken(String refreshToken) {
        OAuthClientProperties properties = oauthProperties.getClientProperties(OAuthProvider.KAKAO);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", properties.getClientId());
        body.add("refresh_token", refreshToken);
        body.add("client_secret", properties.getClientSecret());

        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                throw mapKakaoException(e);
            }
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException(OAuthErrorCode.SOCIAL_SERVER_ERROR);
        }
    }
}
