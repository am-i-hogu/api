package com.hogu.am_i_hogu.domain.user.repository;

import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.dto.MyPageSummary;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);

    Optional<User> findByIdAndIsDeletedFalse(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockByIdAndIsDeletedFalse(Long userId);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.MyPageSummary(
            u.nickname,
            u.profileImageUrl,
            s.votedPostCount,
            s.hoguIndex
            )
            FROM User u
            JOIN UserHoguStat s ON s.userId = u.id
            WHERE u.id = :userId
                AND u.isDeleted = false
            """)
    Optional<MyPageSummary> findMyPageSummaryByUserId(@Param("userId") Long userId);
}
