package com.hogu.am_i_hogu.domain.comment.exception;

import com.hogu.am_i_hogu.common.exception.ErrorCodeType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentErrorCode implements ErrorCodeType {
    EMPTY_REQUEST_BODY(HttpStatus.BAD_REQUEST, "EMPTY_REQUEST_BODY"),
    WRONG_PARENTID_TYPE(HttpStatus.BAD_REQUEST, "WRONG_PARENTID_TYPE"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE"),
    PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PARENT_NOT_FOUND"),
    PARENT_ALREADY_DELETED(HttpStatus.NOT_FOUND, "PARENT_ALREADY_DELETED");

    private final HttpStatus status;
    private final String code;

    CommentErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }
}
