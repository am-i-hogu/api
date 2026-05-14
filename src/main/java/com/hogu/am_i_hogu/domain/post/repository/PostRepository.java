package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.user.dto.MyPostSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Transactional
    @Modifying
    @Query("""
            UPDATE Post p
            SET p.viewCount = p.viewCount + 1
            WHERE p.id = :postId
            """)
    int increaseViewCount(@Param("postId") Long postId);

    @Query("""
            SELECT p.viewCount
            FROM Post p
            WHERE p.id = :postId
            """)
    Optional<Integer> findViewCountById(@Param("postId") Long postId);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.MyPostSummary(
                p.id,
                p.title,
                p.createdAt,
                SUM(CASE WHEN pv.myVote = 'HOGU' THEN 1 ELSE 0 END),
                SUM(CASE WHEN pv.myVote = 'NOT_HOGU' THEN 1 ELSE 0 END)
            )
            FROM Post p
            LEFT JOIN PostVote pv ON pv.id.postId = p.id
            WHERE p.writer.id = :userId
                AND p.isDeleted = false
                AND (
                    :cursorCreatedAt IS NULL
                    OR p.createdAt < :cursorCreatedAt
                    OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorPostId)
                )
            GROUP BY p.id
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<MyPostSummary> findMyPosts(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorPostId") Long cursorPostId,
            Pageable pageable
    );
}
