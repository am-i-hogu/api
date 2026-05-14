package com.hogu.am_i_hogu.common.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CursorCodec {

    private final ObjectMapper objectMapper;

    public CursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> String encode(T cursor) {
        try {
            String json = objectMapper.writeValueAsString(cursor);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public <T> T decode(String cursor, Class<T> type) {
        try {
            String json = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );

            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode cursor", e);
        }
    }
}
