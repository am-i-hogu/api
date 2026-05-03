package com.hogu.am_i_hogu.domain.User.repository;

import com.hogu.am_i_hogu.domain.User.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
