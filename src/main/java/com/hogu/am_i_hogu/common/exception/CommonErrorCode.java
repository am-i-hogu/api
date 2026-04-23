package com.hogu.am_i_hogu.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode {
    // 공통으로 사용될 에러들 Enum 타입으로 정의
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED"),
    EMPTY_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "EMPTY_TOKEN_EXPIRED"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN"),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR");

    private final HttpStatus status;
    private final String code;

    // enum 생성자
    CommonErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
