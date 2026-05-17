package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.UserHoguStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHoguStatRepository extends JpaRepository<UserHoguStat, Long> {
}