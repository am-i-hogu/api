package com.hogu.am_i_hogu.domain.oauth.repository;

import com.hogu.am_i_hogu.domain.oauth.domain.RegisterSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisterSessionRepository extends JpaRepository<RegisterSession, Long> {
}
