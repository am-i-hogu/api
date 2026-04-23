package com.hogu.am_i_hogu.common.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CommonException extends RuntimeException {
    private final CommonErrorCode code;
    private final List<ErrorResponse.ErrorDetail> errors;

    public CommonException(CommonErrorCode code) {
        super(code.getCode());
        this.code = code;
        this.errors = null;
    }

    public CommonException(CommonErrorCode code, List<ErrorResponse.ErrorDetail> errors) {
        super(code.getCode());
        this.code = code;
        this.errors = errors;
    }
}
