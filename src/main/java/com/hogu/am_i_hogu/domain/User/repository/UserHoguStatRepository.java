package com.hogu.am_i_hogu.domain.User.repository;

import com.hogu.am_i_hogu.domain.User.domain.UserHoguStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHoguStatRepository extends JpaRepository<UserHoguStat, Long> {
}
