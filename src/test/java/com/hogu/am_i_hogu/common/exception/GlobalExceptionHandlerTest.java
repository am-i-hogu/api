package com.hogu.am_i_hogu.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    private enum TestErrorCode implements ErrorCodeType {
        TEST_ERROR_CODE(HttpStatus.UNAUTHORIZED, "TEST_ERROR_CODE");

        private final HttpStatus status;
        private final String code;

        TestErrorCode(HttpStatus status, String code) {
            this.status = status;
            this.code = code;
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }

        @Override
        public String getCode() {
            return code;
        }
    }

    private static class TestDomainException extends CustomException {
        public TestDomainException(TestErrorCode code) {
            super(code);
        }
    }

    @Test
    void domainExceptionTest() {
        TestDomainException exception = new TestDomainException(TestErrorCode.TEST_ERROR_CODE);

        // 도메인 예외가 공통 오류 응답으로 변환되는지 검증
        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleCustomException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("TEST_ERROR_CODE");
        assertThat(response.getBody().getErrors()).isNull();
    }

    @Test
    void returnsServerErrorForUnhandledException() {
        Exception exception = new Exception();

        // exception이 500 응답으로 변환되는지 검증
        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleInternalServerError(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("SERVER_ERROR");
        assertThat(response.getBody().getErrors()).isNull();
    }
}
