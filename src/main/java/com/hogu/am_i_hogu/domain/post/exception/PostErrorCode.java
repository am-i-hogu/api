package com.hogu.am_i_hogu.domain.post.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PostErrorCode implements ErrorCodeType {
    EMPTY_REQUEST_BODY(HttpStatus.BAD_REQUEST, "EMPTY_REQUEST_BODY"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE");

    private final HttpStatus status;
    private final String code;

    PostErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
