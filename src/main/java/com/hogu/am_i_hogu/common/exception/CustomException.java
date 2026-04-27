package com.hogu.am_i_hogu.common.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCodeType errorCode;
    private final List<ErrorResponse.ErrorDetail> errors;

    // 상세 오류 없이 throw 할 때 사용
    public CustomException(ErrorCodeType errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.errors = null;
    }

    // 상세 오류 리스트와 함께 throw 할 때 사용
    public CustomException(ErrorCodeType errorCode, List<ErrorResponse.ErrorDetail> errors) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.errors = errors;
    }
}
