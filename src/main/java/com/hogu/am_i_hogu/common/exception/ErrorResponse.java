package com.hogu.am_i_hogu.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 에러 응답", requiredProperties = {"code"})
public class ErrorResponse {

    @Schema(description = "에러 코드")
    private String code;

    @Schema(description = "상세 에러(없을 경우 생략)", nullable = true)
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
    @Schema(description = "필드별 에러 상세", requiredProperties = {"field", "code"})
    public static class ErrorDetail {
        @Schema(description = "에러 발생한 필드명")
        private String field;

        @Schema(description = "해당 필드의 상세 에러 코드")
        private String code;

        public ErrorDetail(String field, String code) {
            this.field = field;
            this.code = code;
        }
    }
}
