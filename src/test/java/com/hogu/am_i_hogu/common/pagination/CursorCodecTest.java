package com.hogu.am_i_hogu.common.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hogu.am_i_hogu.domain.user.dto.MyPostCursor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class CursorCodecTest {

    private final CursorCodec cursorCodec = new CursorCodec(
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    /**
     * 유효한 cursor 문자열을 디코딩하는 경우 테스트:
     * 원본 cursor 객체로 복원되는지 확인
     */
    @Test
    void decodeReturnsCursorWhenCursorIsValid() {
        MyPostCursor cursor = new MyPostCursor(
                LocalDateTime.of(2026, 5, 1, 9, 0, 0),
                123L
        );
        String encodedCursor = cursorCodec.encode(cursor);

        MyPostCursor decodedCursor = cursorCodec.decode(encodedCursor, MyPostCursor.class);

        assertThat(decodedCursor.createdAt()).isEqualTo(cursor.createdAt());
        assertThat(decodedCursor.postId()).isEqualTo(cursor.postId());
    }

    /**
     * 유효하지 않은 cursor 문자열을 디코딩하는 경우 테스트:
     * IllegalStateException이 발생하는지 확인
     */
    @Test
    void decodeReturnsIllegalStateExceptionWhenCursorIsInvalid() {
        assertThatThrownBy(() -> cursorCodec.decode("invalid-cursor", MyPostCursor.class))
                .isInstanceOf(IllegalStateException.class);
    }
}
