package com.hogu.am_i_hogu.domain.comment.repository;

import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import com.hogu.am_i_hogu.domain.user.dto.MyCommentSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount(
                c.post.id,
                COUNT(c)
            )
            FROM Comment c
            WHERE c.post.id IN :postIds
                AND c.isDeleted = false
            GROUP BY c.post.id
            """)
    List<PostCommentCount> countCommentsGroupedByPostIds(@Param("postIds") List<Long> postIds);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.MyCommentSummary(
                c.id,
                c.content,
                c.createdAt,
                p.id,
                p.title,
                p.isDeleted
            )
            FROM Comment c
            JOIN c.post p
            WHERE c.writer.id = :userId
                AND c.isDeleted = false
                AND (
                    :cursorCreatedAt IS NULL
                    OR c.createdAt < :cursorCreatedAt
                    OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorCommentId)
                )
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<MyCommentSummary> findMyComments(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorCommentId") LocalDateTime cursorCommentId,
            Pageable pageable
    );
}
