package com.hogu.am_i_hogu.domain.oauth.repository;

import com.hogu.am_i_hogu.domain.oauth.domain.OAuthLoginState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * OAuth 로그인 과정에서 발생하는 상태값의 영속성 관리
 * 인가 요청 전 DB에 상태 저장을 위해 사용
 */
@Repository
public interface OAuthLoginStateRepository extends JpaRepository<OAuthLoginState, Long> {
    OAuthLoginState findByState(Long state);
}
