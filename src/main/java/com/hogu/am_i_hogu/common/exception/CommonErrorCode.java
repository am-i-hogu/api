package com.hogu.am_i_hogu.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode {
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR");

    private final HttpStatus status;
    private final String code;

    CommonErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}

