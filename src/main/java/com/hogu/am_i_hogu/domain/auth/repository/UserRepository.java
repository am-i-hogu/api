package com.hogu.am_i_hogu.domain.auth.repository;

import com.hogu.am_i_hogu.domain.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
