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
@Table(name="register_sessions")
public class RegisterSession {
    private static final long EXPIRES_IN_MINUTES = 10;
    @Id
    Long id;

    @Column(nullable = false)
    Long socialAccountId;

    @Column(nullable = false, length = 255)
    String registerTokenHash;

    @Column(nullable = false)
    LocalDateTime expiresAt;

    LocalDateTime consumedAt;

    @Column(nullable = false)
    LocalDateTime createdAt;

    public RegisterSession(
            Long id,
            Long socialAccountId,
            String registerTokenHash,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.socialAccountId = socialAccountId;
        this.registerTokenHash = registerTokenHash;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusMinutes(EXPIRES_IN_MINUTES);
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void markConsumed(LocalDateTime now) {
        this.consumedAt = now;
    }
}
