package com.hogu.am_i_hogu.domain.oauth.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OAuthErrorCode implements ErrorCodeType {
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER");

    private final HttpStatus status;
    private final String code;

    OAuthErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
