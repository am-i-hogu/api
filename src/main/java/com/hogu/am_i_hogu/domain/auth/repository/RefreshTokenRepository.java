package com.hogu.am_i_hogu.domain.auth.repository;

import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * refresh token의 영속성 관리
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
