package com.hogu.am_i_hogu.common.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCodeType code;
    private final List<ErrorResponse.ErrorDetail> errors;

    // 상세 오류 없이 throw 할 때 사용
    public CustomException(ErrorCodeType code) {
        super(code.getCode());
        this.code = code;
        this.errors = null;
    }

    // 상세 오류 리스트와 함께 throw 할 때 사용
    public CustomException(ErrorCodeType code, List<ErrorResponse.ErrorDetail> errors) {
        super(code.getCode());
        this.code = code;
        this.errors = errors;
    }
}
