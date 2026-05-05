package com.hogu.am_i_hogu.domain.auth.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCodeType {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE"),
    EMPTY_NICKNAME(HttpStatus.BAD_REQUEST, "EMPTY_NICKNAME"),
    SPECIAL_CHAR_NICKNAME(HttpStatus.BAD_REQUEST, "SPECIAL_CHAR_NICKNAME"),
    NICKNAME_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "NICKNAME_LENGTH_EXCEEDED"),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "DUPLICATE_NICKNAME"),
    REGISTER_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REGISTER_TOKEN_EXPIRED"),
    EMPTY_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "EMPTY_REGISTER_TOKEN"),
    INVALID_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REGISTER_TOKEN");

    private final HttpStatus status;
    private final String code;

    AuthErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
