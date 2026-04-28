package com.hogu.am_i_hogu.domain.oauth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OAuth 로그인 과정에서 발급한 state, nonce 값을 저장하기 위한 엔티티
 */
@Getter
@Entity
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Table(name="oauth_login_states")
public class OAuthLoginState {
    private static final long EXPIRES_IN_MINUTES = 5;       // TODO: state 만료 시간 논의 필요
    @Id
    private Long id;                                        // TODO: 공통 ID 생성 전략 적용 필요

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OAuthProvider provider;

    @Column(nullable = false, length = 64, unique = true)
    private String state;

    @Column(nullable = false, length = 64, unique = true)
    private String nonce;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime consumedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public OAuthLoginState(
            Long id,
            OAuthProvider provider,
            String state,
            String nonce,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.provider = provider;
        this.state = state;
        this.nonce = nonce;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusMinutes(EXPIRES_IN_MINUTES);
    }
}
