package com.hogu.am_i_hogu.domain.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {
    @Id
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String nickname;

    @Column(length = 512)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public User(
            Long id,
            String nickname,
            boolean isDeleted,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.nickname = nickname;
        this.profileImageUrl = null;
        this.isDeleted = isDeleted;
        this.deletedAt = null;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
}
