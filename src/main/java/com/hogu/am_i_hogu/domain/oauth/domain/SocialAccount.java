package com.hogu.am_i_hogu.domain.oauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "social_accounts")
public class SocialAccount {
    @Id
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OAuthProvider provider;

    @Column(nullable = false, length = 255)
    private String providerUserId;

    private LocalDateTime linkedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public SocialAccount(
            Long id,
            OAuthProvider provider,
            String providerUserId,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = createdAt;
    }

    public boolean isLinked() {
        return userId != null;
    }

    public void linkToUser(Long userId, LocalDateTime now) {
        this.userId = userId;
        this.linkedAt = now;
    }
}
