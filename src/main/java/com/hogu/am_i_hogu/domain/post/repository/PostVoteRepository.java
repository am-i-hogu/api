package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.PostVote;
import com.hogu.am_i_hogu.domain.post.domain.PostVoteId;
import com.hogu.am_i_hogu.domain.post.dto.PostVoteCounts;
import com.hogu.am_i_hogu.domain.user.dto.MyVoteSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, PostVoteId> {

    @Modifying
    @Query(value = """
            INSERT INTO post_votes
                (user_id, post_id, my_vote, created_at, updated_at)
            VALUES
                (:userId, :postId, :myVote, :now, :now)
            ON DUPLICATE KEY UPDATE
                my_vote = VALUES(my_vote),
                updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    int upsertVote(
            @Param("userId") Long userId,
            @Param("postId") Long postId,
            @Param("myVote") String myVote,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.post.dto.PostVoteCounts(
                SUM(CASE WHEN pv.myVote = 'HOGU' THEN 1 ELSE 0 END),
                SUM(CASE WHEN pv.myVote = 'NOT_HOGU' THEN 1 ELSE 0 END)
            )
            FROM PostVote pv
            WHERE pv.id.postId = :postId
            """)
    PostVoteCounts countByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.post.dto.PostVoteCounts(
                SUM(CASE WHEN pv.myVote = 'HOGU' THEN 1 ELSE 0 END),
                SUM(CASE WHEN pv.myVote = 'NOT_HOGU' THEN 1 ELSE 0 END),
                COUNT(DISTINCT p.id)
            )
            FROM PostVote pv
            JOIN Post p ON p.id = pv.id.postId
            WHERE p.writer.id = :writerUserId
              AND p.isDeleted = false
              AND pv.myVote IN ('HOGU', 'NOT_HOGU')
            """)
    PostVoteCounts countByWriterUserId(@Param("writerUserId") Long writerUserId);

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
                AND pv.myVote <> 'NONE'
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
