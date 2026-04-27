package com.hogu.am_i_hogu.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // CustomException으로 전달된 경우 공통 응답 형식으로 변환
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCodeType code = e.getErrorCode();
        ErrorResponse body = (e.getErrors() == null || e.getErrors().isEmpty())
                ? ErrorResponse.of(code)
                : ErrorResponse.of(code, e.getErrors());

        return ResponseEntity
                .status(code.getStatus())
                .body(body);
    }

    // 처리되지 않은 예외는 500 Internal Server Error로 fallback 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e) {
        log.error("Unhandled Exception occurred. message={}", e.getMessage(), e);
        CommonErrorCode code = CommonErrorCode.SERVER_ERROR;

        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code));
    }
}
