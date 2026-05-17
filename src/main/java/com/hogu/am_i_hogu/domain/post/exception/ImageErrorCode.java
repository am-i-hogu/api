package com.hogu.am_i_hogu.domain.post.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ImageErrorCode implements ErrorCodeType {
    EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "EMPTY_IMAGE_FILE"),
    UNSUPPORTED_FORMAT(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FORMAT"),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_SIZE_EXCEEDED");

    private final HttpStatus status;
    private final String code;

    ImageErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
