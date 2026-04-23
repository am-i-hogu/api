package com.hogu.am_i_hogu.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError (Exception e) {
        log.error("Internal Server Error", e);
        CommonErrorCode code = CommonErrorCode.SERVER_ERROR;

        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code));
    }
}
