package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.config.OAuthClientProperties;
import com.hogu.am_i_hogu.domain.oauth.config.OAuthProperties;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class IdTokenVerifier {
    private final JwtDecoder googleIdTokenJwtDecoder;
    private final OAuthProperties oauthProperties;

    public IdTokenVerifier(
            JwtDecoder googleIdTokenJwtDecoder,
            OAuthProperties oauthProperties
    ) {
        this.googleIdTokenJwtDecoder = googleIdTokenJwtDecoder;
        this.oauthProperties = oauthProperties;
    }

    /**
     * Google id token 서명 및 claim 검증
     * @param idToken       소셜 서버로부터 받아온 idToken
     * @param expectedNonce 우리 서버에서 발급해 저장해둔 nonce 값
     * @return 검증이 완료된 Jwt 객체
     */
    public Jwt verify(String idToken, String expectedNonce) {
        try {
            OAuthClientProperties properties = oauthProperties.getClientProperties(OAuthProvider.GOOGLE);
            Jwt jwt = googleIdTokenJwtDecoder.decode(idToken);

            String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            List<String> aud = jwt.getAudience();
            String nonce = jwt.getClaimAsString("nonce");
            Instant expiresAt = jwt.getExpiresAt();

            List<String> allowedIssuers = properties.getIssuerUris();

            if (allowedIssuers == null
                    || !allowedIssuers.contains(iss)
                    || aud == null
                    || !aud.contains(properties.getClientId())
                    || nonce == null
                    || !nonce.equals(expectedNonce)
                    || expiresAt == null
                    || expiresAt.isBefore(Instant.now())) {
                throw new CustomException(OAuthErrorCode.INVALID_ID_TOKEN);
            }

            return jwt;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(OAuthErrorCode.INVALID_ID_TOKEN);
        }
    }
}
