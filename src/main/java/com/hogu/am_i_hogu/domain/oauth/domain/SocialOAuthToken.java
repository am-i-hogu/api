package com.hogu.am_i_hogu.domain.oauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="social_oauth_tokens")
public class SocialOAuthToken {
    @Id
    Long id;

    @Column(nullable = false)
    Long socialAccountId;

    @Column(nullable = false)
    String accessTokenEncrypted;

    @Column(nullable = false)
    String refreshTokenEncrypted;

    @Column(nullable = false)
    LocalDateTime accessTokenExpiresAt;

    @Column(nullable = false)
    LocalDateTime refreshTokenExpiresAt;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
    LocalDateTime updatedAt;

    public SocialOAuthToken(
            Long id,
            Long socialAccountId,
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            LocalDateTime accessTokenExpiresAt,
            LocalDateTime refreshTokenExpiresAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.socialAccountId = socialAccountId;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

}
