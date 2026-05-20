package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.dto.PostVoteCounts;
import com.hogu.am_i_hogu.domain.post.repository.PostVoteRepository;
import com.hogu.am_i_hogu.domain.user.domain.UserHoguStat;
import com.hogu.am_i_hogu.domain.user.repository.UserHoguStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WriterHoguStatService {
    private final PostVoteRepository postVoteRepository;
    private final UserHoguStatRepository userHoguStatRepository;

    @Transactional
    public void recalculate(Long writerUserId, LocalDateTime now) {
        PostVoteCounts voteCounts = postVoteRepository.countByWriterUserId(writerUserId);
        int votedPostCount = Math.toIntExact(voteCounts.votedPostCount());
        int hoguVoteCount = Math.toIntExact(voteCounts.hoguVoteCount());
        int totalVoteCount = Math.toIntExact(voteCounts.totalVoteCount());

        UserHoguStat stat = userHoguStatRepository.findById(writerUserId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));
        stat.updateVoteStats(votedPostCount, hoguVoteCount, totalVoteCount, now);
        userHoguStatRepository.save(stat);
    }
}
