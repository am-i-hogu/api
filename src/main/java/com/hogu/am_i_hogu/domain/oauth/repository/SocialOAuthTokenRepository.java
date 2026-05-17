package com.hogu.am_i_hogu.domain.oauth.repository;

import com.hogu.am_i_hogu.domain.oauth.domain.SocialOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialOAuthTokenRepository extends JpaRepository<SocialOAuthToken, Long> {
    Optional<SocialOAuthToken> findBySocialAccountId(Long socialAccountId);
}
