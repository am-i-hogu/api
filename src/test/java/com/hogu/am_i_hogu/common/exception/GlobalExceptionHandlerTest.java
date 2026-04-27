package com.hogu.am_i_hogu.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    // 테스트를 위한 임시 오류 코드
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

    // 테스트를 위한 임시 Exception
    private static class TestCustomException extends CustomException {
        public TestCustomException(TestErrorCode code) {
            super(code);
        }
    }

    /**
     * 커스텀 예외가 공통 오류 응답으로 변환되는지 검증
     */
    @Test
    void customExceptionTest() {
        TestCustomException exception = new TestCustomException(TestErrorCode.TEST_ERROR_CODE);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleCustomException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("TEST_ERROR_CODE");
        assertThat(response.getBody().getErrors()).isNull();
    }

    /**
     * 처리되지 않은 예외가 500 Internal Server Error로 fallback 처리되는지 검증
     */
    @Test
    void returnsServerErrorForUnhandledException() {
        Exception exception = new Exception();

        // exception이 500 응답으로 변환되는지 검증
        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleUnhandledException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("SERVER_ERROR");
        assertThat(response.getBody().getErrors()).isNull();
    }
}
