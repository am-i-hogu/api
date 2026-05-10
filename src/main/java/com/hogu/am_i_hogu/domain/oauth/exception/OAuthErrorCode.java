package com.hogu.am_i_hogu.domain.oauth.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OAuthErrorCode implements ErrorCodeType {
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER"),
    PROVIDER_MISMATCH(HttpStatus.BAD_REQUEST, "PROVIDER_MISMATCH"),
    INVALID_STATE(HttpStatus.UNAUTHORIZED, "INVALID_STATE"),
    STATE_REUSED(HttpStatus.UNAUTHORIZED, "STATE_REUSED"),
    STATE_EXPIRED(HttpStatus.UNAUTHORIZED, "STATE_EXPIRED"),
    INVALID_AUTH_CODE(HttpStatus.UNAUTHORIZED, "INVALID_AUTH_CODE"),
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ID_TOKEN"),
    SOCIAL_SERVER_REJECTED(HttpStatus.BAD_GATEWAY, "SOCIAL_SERVER_REJECTED"),
    SOCIAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "SOCIAL_SERVER_ERROR");


    private final HttpStatus status;
    private final String code;

    OAuthErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
