package com.hogu.am_i_hogu.domain.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "hogu_levels")
public class HoguLevel {
    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 20)
    private String displayName;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer maxHoguIndex;

    @Column(nullable = false)
    private Integer minHoguIndex;
}
