package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.HoguLevel;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelDetailInfo;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelShortInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HoguLevelRepository extends JpaRepository<HoguLevel, String> {

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.HoguLevelShortInfo(
            h.code,
            h.shortDescription
            )
            FROM HoguLevel h
            WHERE h.minHoguIndex <= :hoguIndex
                AND h.maxHoguIndex >= :hoguIndex
            """)
    Optional<HoguLevelShortInfo> findShortHoguLevelByHoguIndex(@Param("hoguIndex") Integer hoguIndex);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.HoguLevelDetailInfo(
            h.code,
            h.shortDescription,
            h.description
            )
            FROM HoguLevel h
            WHERE h.minHoguIndex <= :hoguIndex
                AND h.maxHoguIndex >= :hoguIndex
            """)
    Optional<HoguLevelDetailInfo> findDetailHoguLevelByHoguIndex(@Param("hoguIndex") Integer hoguIndex);

}
