package com.hogu.am_i_hogu.domain.auth.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCodeType {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE"),
    REGISTER_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REGISTER_TOKEN_EXPIRED"),
    EMPTY_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "EMPTY_REGISTER_TOKEN"),
    INVALID_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REGISTER_TOKEN"),
    EMPTY_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EMPTY_REFRESH_TOKEN"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED,"REFRESH_TOKEN_REUSED"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME");

    private final HttpStatus status;
    private final String code;

    AuthErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
