package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.PostBookmark;
import com.hogu.am_i_hogu.domain.post.domain.PostBookmarkId;
import com.hogu.am_i_hogu.domain.user.dto.MyBookmarkSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmarkId> {

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.MyBookmarkSummary(
                p.id,
                p.title,
                p.category.code,
                p.createdAt,
                pb.createdAt,
                SUM(CASE WHEN pv.myVote = 'HOGU' THEN 1 ELSE 0 END),
                SUM(CASE WHEN pv.myVote = 'NOT_HOGU' THEN 1 ELSE 0 END)
            )
            FROM PostBookmark pb
            JOIN Post p ON p.id = pb.id.postId
            LEFT JOIN PostVote pv ON pv.id.postId = p.id
            WHERE pb.id.userId = :userId
                AND (
                    :cursorCreatedAt IS NULL
                    OR pb.createdAt < :cursorCreatedAt
                    OR (pb.createdAt = :cursorCreatedAt AND p.id < :cursorPostId)
                )
            GROUP BY p.id
            ORDER BY pb.createdAt DESC, p.id DESC
            """)
    List<MyBookmarkSummary> findMyBookmarks(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorPostId") Long cursorPostId,
            Pageable pageable
    );
}
