package com.hogu.am_i_hogu.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String code;
    private List<ErrorDetail> errors;

    // 상세 오류 리스트가 없는 에러 응답을 위한 생성자
    public ErrorResponse(String code) {
        this.code = code;
        this.errors = null;
    }

    // 상세 오류 리스트가 있는 에러 응답을 위한 생성자
    public ErrorResponse(String code, List<ErrorDetail> errors) {
        this.code = code;
        this.errors = errors;
    }

    // 상세 오류 리스트가 없는 에러 응답을 위한 응답 본문 생성
    public static ErrorResponse of(ErrorCodeType code) {
        return new ErrorResponse(code.getCode());
    }

    // 상세 오류 리스트가 있는 에러 응답을 위한 응답 본문 생성
    public static ErrorResponse of(ErrorCodeType code, List<ErrorDetail> errors) {
        return new ErrorResponse(code.getCode(), errors);
    }

    // 상세 오류 리스트 클래스
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
