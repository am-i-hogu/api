package com.hogu.am_i_hogu.domain.oauth.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OAuthErrorCode implements ErrorCodeType {
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE"),
    EMPTY_NICKNAME(HttpStatus.BAD_REQUEST, "EMPTY_NICKNAME"),
    SPECIAL_CHAR_NICKNAME(HttpStatus.BAD_REQUEST, "SPECIAL_CHAR_NICKNAME"),
    NICKNAME_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "NICKNAME_LENGTH_EXCEEDED"),
    INVALID_STATE(HttpStatus.UNAUTHORIZED, "INVALID_STATE"),
    STATE_REUSED(HttpStatus.UNAUTHORIZED, "STATE_REUSED"),
    STATE_EXPIRED(HttpStatus.UNAUTHORIZED, "STATE_EXPIRED"),
    INVALID_AUTH_CODE(HttpStatus.UNAUTHORIZED, "INVALID_AUTH_CODE"),
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ID_TOKEN"),
    REGISTER_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REGISTER_TOKEN_EXPIRED"),
    EMPTY_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "EMPTY_REGISTER_TOKEN"),
    INVALID_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REGISTER_TOKEN"),
    SOCIAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "SOCIAL_SERVER_ERROR");


    private final HttpStatus status;
    private final String code;

    OAuthErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
