package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);
    Optional<User> findByIdAndIsDeletedFalse(Long userId);
}
