package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.PostVote;
import com.hogu.am_i_hogu.domain.post.domain.PostVoteId;
import com.hogu.am_i_hogu.domain.user.dto.MyVoteSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, PostVoteId> {

    @Query("""
            SELECT COUNT(pv)
            FROM PostVote pv
            WHERE pv.id.postId = :postId
              AND pv.myVote = :myVote
            """)
    long countByPostIdAndMyVote(@Param("postId") Long postId, @Param("myVote") String myVote);

    @Query("""
            SELECT pv
            FROM PostVote pv
            WHERE pv.id.postId = :postId
              AND pv.id.userId = :userId
            """)
    // 아직 투표나 투표 취소를 안했을 경우에 NULL일 수 있으므로 Optional로 설정한다.
    Optional<PostVote> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.MyVoteSummary(
                pv.myVote,
                pv.createdAt,
                p.id,
                p.title,
                p.category.code,
                p.isDeleted
            )
            FROM PostVote pv
            JOIN Post p ON p.id = pv.id.postId
            WHERE pv.id.userId = :userId
                AND (
                    :cursorCreatedAt IS NULL
                    OR pv.createdAt < :cursorCreatedAt
                    OR (pv.createdAt = :cursorCreatedAt AND p.id < :cursorPostId)
                )
            ORDER BY pv.createdAt DESC, p.id DESC
            """)
    List<MyVoteSummary> findMyVotes(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorPostId") Long cursorPostId,
            Pageable pageable
    );
}
