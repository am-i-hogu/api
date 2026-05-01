package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
