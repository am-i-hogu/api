package com.hogu.am_i_hogu.domain.user.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements ErrorCodeType {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE"),
    EMPTY_REQUEST_BODY(HttpStatus.BAD_REQUEST, "EMPTY_REQUEST_BODY");
    private final HttpStatus status;
    private final String code;

    UserErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
