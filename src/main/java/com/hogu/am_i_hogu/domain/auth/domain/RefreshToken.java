package com.hogu.am_i_hogu.domain.auth.domain;

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
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken {
    private static final long EXPIRES_IN_DAYS = 7;
    @Id
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String tokenHash;

    @Column(nullable = false)
    private boolean isRevoked;

    @Column(nullable = false)
    private boolean isRotated;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public RefreshToken(
            Long id,
            Long userId,
            String tokenHash,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.isRevoked = false;
        this.isRotated = false;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusDays(EXPIRES_IN_DAYS);
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now) {
        this.isRevoked = true;
        this.revokedAt = now;
    }

    public void markRotated() {
        this.isRotated = true;
    }
}
