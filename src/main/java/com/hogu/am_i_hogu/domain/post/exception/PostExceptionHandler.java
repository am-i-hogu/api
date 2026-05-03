package com.hogu.am_i_hogu.domain.post.exception;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.post.controller.PostController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PostController.class)
public class PostExceptionHandler {

    /**
     * PostController의 postId path variable이 Long으로 변환되지 못한 경우 명세의 오류 코드로 응답한다.
     *
     * @param e path variable 타입 변환 실패 예외
     * @return WRONG_POSTID_TYPE 오류 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        PostErrorCode code = PostErrorCode.WRONG_POSTID_TYPE;

        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code));
    }
}
