package com.hogu.am_i_hogu.domain.oauth.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUnlinkErrorResponse {
    private Integer code;
    private String msg;
}