package com.hogu.am_i_hogu.domain.comment.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentErrorCode implements ErrorCodeType {
    EMPTY_REQUEST_BODY(HttpStatus.BAD_REQUEST, "EMPTY_REQUEST_BODY"),
    WRONG_PARENTID_TYPE(HttpStatus.BAD_REQUEST, "WRONG_PARENTID_TYPE"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE"),
    INVALID_PARAM_VALUE(HttpStatus.BAD_REQUEST, "INVALID_PARAM_VALUE"),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "EMPTY_CONTENT"),
    CONTENT_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CONTENT_LENGTH_EXCEEDED"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND"),
    COMMENT_ALREADY_DELETED(HttpStatus.NOT_FOUND, "COMMENT_ALREADY_DELETED"),
    PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PARENT_NOT_FOUND"),
    PARENT_ALREADY_DELETED(HttpStatus.NOT_FOUND, "PARENT_ALREADY_DELETED");

    private final HttpStatus status;
    private final String code;

    CommentErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
