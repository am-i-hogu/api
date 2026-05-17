package com.hogu.am_i_hogu.domain.policy.repository;

import com.hogu.am_i_hogu.domain.policy.domain.PolicyRevisions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRevisionRepository extends JpaRepository<PolicyRevisions, Long> {
    Optional<PolicyRevisions> findTopByPolicyTypeAndIsCurrentTrueOrderByUpdatedAtDesc(String policyType);
}
