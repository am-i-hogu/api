package com.hogu.am_i_hogu.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCodeType {
    HttpStatus getStatus();
    String getCode();
}
