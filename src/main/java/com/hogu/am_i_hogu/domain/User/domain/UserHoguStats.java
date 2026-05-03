package com.hogu.am_i_hogu.domain.User.domain;

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
public class UserHoguStats {
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

    public UserHoguStats(
            Long userId,
            LocalDateTime updatedAt
    ) {
        this.userId = userId;
        this.updatedAt = updatedAt;
    }
}
