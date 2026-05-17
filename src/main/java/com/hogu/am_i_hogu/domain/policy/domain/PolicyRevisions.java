package com.hogu.am_i_hogu.domain.policy.domain;

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
@Table(name="policy_revisions")
public class PolicyRevisions {
    @Id
    private Long id;

    @Column(name = "policy_type", nullable = false, length = 20)
    private String policyType;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    @Column(nullable = false)
    private boolean isCurrent;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
