package com.hogu.am_i_hogu.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String code;
    private List<ErrorDetail> errors;

    public ErrorResponse(String code) {
        this.code = code;
    }

    public ErrorResponse(String code, List<ErrorDetail> errors) {
        this.code = code;
        this.errors = errors;
    }

    public static ErrorResponse of(CommonErrorCode code) {
        return new ErrorResponse(code.getCode());
    }

    public static ErrorResponse of(CommonErrorCode code, List<ErrorDetail> errors) {
        return new ErrorResponse(code.getCode(), errors);
    }

    @Getter
    public static class ErrorDetail {
        private String field;
        private String code;

        public ErrorDetail(String field, String code) {
            this.field = field;
            this.code = code;
        }
    }
}
