package com.hogu.am_i_hogu.domain.oauth.repository;

import com.hogu.am_i_hogu.domain.oauth.domain.RegisterSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원가입 세션의 영속성 관리
 */
public interface RegisterSessionRepository extends JpaRepository<RegisterSession, Long> {
    Optional<RegisterSession> findBySocialAccountId(Long socialAccountId);
}
