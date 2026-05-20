package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.dto.PostVoteCounts;
import com.hogu.am_i_hogu.domain.post.repository.PostVoteRepository;
import com.hogu.am_i_hogu.domain.user.domain.UserHoguStat;
import com.hogu.am_i_hogu.domain.user.repository.UserHoguStatRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WriterHoguStatServiceTest {
    private final PostVoteRepository postVoteRepository = mock(PostVoteRepository.class);
    private final UserHoguStatRepository userHoguStatRepository = mock(UserHoguStatRepository.class);
    private final WriterHoguStatService writerHoguStatService = new WriterHoguStatService(
            postVoteRepository,
            userHoguStatRepository
    );

    // 정상 케이스: 작성자 통계 row가 이미 있으면 투표 재집계 결과로 값을 갱신하고 저장한다.
    @Test
    void recalculateUpdatesExistingWriterStat() {
        Long writerUserId = 1L;
        LocalDateTime now = LocalDateTime.now();
        UserHoguStat stat = new UserHoguStat(writerUserId, now);
        when(postVoteRepository.countByWriterUserId(writerUserId))
                .thenReturn(new PostVoteCounts(2L, 3L, 4L));
        when(userHoguStatRepository.findById(writerUserId))
                .thenReturn(Optional.of(stat));

        writerHoguStatService.recalculate(writerUserId, now);

        assertThat(stat.getVotedPostCount()).isEqualTo(4);
        assertThat(stat.getHoguVoteCount()).isEqualTo(2);
        assertThat(stat.getTotalVoteCount()).isEqualTo(5);
        assertThat(stat.getHoguIndex()).isEqualTo(40);
        verify(userHoguStatRepository).save(stat);
    }

    // 실패 케이스: 정상 온보딩 유저라면 있어야 할 작성자 통계 row가 없으면 SERVER_ERROR를 반환한다.
    @Test
    void recalculateThrowsServerErrorWhenWriterStatDoesNotExist() {
        Long writerUserId = 1L;
        LocalDateTime now = LocalDateTime.now();
        when(postVoteRepository.countByWriterUserId(writerUserId))
                .thenReturn(new PostVoteCounts(0L, 0L, 0L));
        when(userHoguStatRepository.findById(writerUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> writerHoguStatService.recalculate(writerUserId, now))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.SERVER_ERROR);

        verify(userHoguStatRepository, never()).save(any(UserHoguStat.class));
    }
}
