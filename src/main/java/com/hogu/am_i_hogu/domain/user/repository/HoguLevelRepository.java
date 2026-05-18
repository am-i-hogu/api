package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.HoguLevel;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HoguLevelRepository extends JpaRepository<HoguLevel, String> {

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.HoguLevelInfo(
            h.displayName,
            h.description
            )
            FROM HoguLevel h
            WHERE h.minHoguIndex <= :hoguIndex
                AND h.maxHoguIndex >= :hoguIndex
            """)
    Optional<HoguLevelInfo> findHoguLevelByHoguIndex(@Param("hoguIndex") Integer hoguIndex);
}
