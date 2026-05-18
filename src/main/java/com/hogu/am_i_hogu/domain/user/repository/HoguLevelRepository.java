package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.HoguLevel;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelInfo;
import com.hogu.am_i_hogu.domain.user.dto.SimpleHoguLevelInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HoguLevelRepository extends JpaRepository<HoguLevel, String> {

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.SimpleHoguLevelInfo(
            h.code,
            h.shortDescription
            )
            FROM HoguLevel h
            WHERE h.minHoguIndex <= :hoguIndex
                AND h.maxHoguIndex >= :hoguIndex
            """)
    Optional<SimpleHoguLevelInfo> findSimpleHoguLevelByHoguIndex(@Param("hoguIndex") Integer hoguIndex);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.HoguLevelInfo(
            h.code,
            h.shortDescription,
            h.description
            )
            FROM HoguLevel h
            WHERE h.minHoguIndex <= :hoguIndex
                AND h.maxHoguIndex >= :hoguIndex
            """)
    Optional<HoguLevelInfo> findHoguLevelByHoguIndex(@Param("hoguIndex") Integer hoguIndex);

}
