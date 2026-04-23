package com.hogu.am_i_hogu.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void returnsServerErrorForUnhandledException() {
        Exception exception = new RuntimeException("unexpected");

        // exception이 500 응답으로 변환되는지 검증
        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInternalServerError(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("SERVER_ERROR");
        assertThat(response.getBody().getErrors()).isNull();
    }
}
