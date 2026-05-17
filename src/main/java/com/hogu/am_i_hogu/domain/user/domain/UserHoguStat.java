package com.hogu.am_i_hogu.domain.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(name="user_hogu_stats")
public class UserHoguStat {
    @Id
    private Long userId;

    @Column(nullable = false)
    private Integer hoguVoteCount = 0;

    @Column(nullable = false)
    private Integer totalVoteCount = 0;

    @Column(nullable = false)
    private Integer hoguIndex = 0;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer votedPostCount = 0;

    public UserHoguStat(
            Long userId,
            LocalDateTime updatedAt
    ) {
        this.userId = userId;
        this.updatedAt = updatedAt;
    }

    public void updateVoteStats(
            int hoguVoteCount,
            int totalVoteCount,
            LocalDateTime updatedAt
    ) {
        this.hoguVoteCount = hoguVoteCount;
        this.totalVoteCount = totalVoteCount;
        this.hoguIndex = totalVoteCount == 0
                ? 0
                : (int) Math.round(hoguVoteCount * 100.0 / totalVoteCount);
        this.updatedAt = updatedAt;
    }
}
