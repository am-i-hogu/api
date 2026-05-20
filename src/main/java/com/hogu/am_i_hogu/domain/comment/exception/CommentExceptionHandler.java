package com.hogu.am_i_hogu.domain.comment.exception;

import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.comment.controller.CommentController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CommentController.class)
public class CommentExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String field = e.getName();

        CommentErrorCode code = switch(field) {
            case "postId" -> CommentErrorCode.WRONG_POSTID_TYPE;
            case "commentId" -> CommentErrorCode.WRONG_COMMENTID_TYPE;
            default -> CommentErrorCode.INVALID_PARAM_VALUE;
        };

        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpServletRequest request) {
        CommentErrorCode code = CommentErrorCode.WRONG_PARENTID_TYPE;

        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code));
    }
}
