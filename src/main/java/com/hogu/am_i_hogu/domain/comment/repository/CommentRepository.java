package com.hogu.am_i_hogu.domain.comment.repository;

import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.CommentInfo;
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
            SELECT new com.hogu.am_i_hogu.domain.comment.dto.CommentInfo(
                c.id,
                c.content,
                c.writer.id,
                c.writer.nickname,
                c.writer.profileImageUrl,
                CASE WHEN c.writer.id = p.writer.id THEN true ELSE false END,
                c.createdAt,
                c.updatedAt,
                c.isDeleted,
                c.parentComment.id,
                c.depth,
                COUNT(chm)
            )
            FROM Comment c
            JOIN c.post p
            LEFT JOIN CommentHelpfulMark chm ON chm.id.commentId = c.id
            WHERE p.id = :postId
                AND (
                    :cursorCreatedAt IS NULL
                    OR c.createdAt < :cursorCreatedAt
                    OR (c.createdAt = :cursorCreatedAt AND c.id < :cursorCommentId)
                )
                AND c.depth = 0
            GROUP BY c.id
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<CommentInfo> findParentCommentsByPostIdOrderByLatest(
            @Param("postId") Long postId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorCommentId") Long cursorCommentId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.comment.dto.CommentInfo(
                c.id,
                c.content,
                c.writer.id,
                c.writer.nickname,
                c.writer.profileImageUrl,
                CASE WHEN c.writer.id = p.writer.id THEN true ELSE false END,
                c.createdAt,
                c.updatedAt,
                c.isDeleted,
                c.parentComment.id,
                c.depth,
                COUNT(chm)
            )
            FROM Comment c
            JOIN c.post p
            LEFT JOIN CommentHelpfulMark chm ON chm.id.commentId = c.id
            WHERE p.id = :postId
                AND c.depth = 0
            GROUP BY c.id
            HAVING :cursorHelpfulCount IS NULL
                OR COUNT(chm) < :cursorHelpfulCount
                OR (COUNT(chm) = :cursorHelpfulCount AND c.id < :cursorCommentId)
            ORDER BY COUNT(chm) DESC, c.id DESC
            """)
    List<CommentInfo> findParentCommentsByPostIdOrderByHelpful(
            @Param("postId") Long postId,
            @Param("cursorHelpfulCount") Long cursorHelpfulCount,
            @Param("cursorCommentId") Long cursorCommentId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.comment.dto.CommentInfo(
                c.id,
                c.content,
                c.writer.id,
                c.writer.nickname,
                c.writer.profileImageUrl,
                CASE WHEN c.writer.id = p.writer.id THEN true ELSE false END,
                c.createdAt,
                c.updatedAt,
                c.isDeleted,
                c.parentComment.id,
                c.depth,
                COUNT(chm)
            )
            FROM Comment c
            JOIN c.post p
            LEFT JOIN CommentHelpfulMark chm ON chm.id.commentId = c.id
            WHERE c.parentComment.id IN :parentIds
            GROUP BY c.id
            ORDER BY c.parentComment.id ASC, c.createdAt ASC, c.id ASC
            """)
    List<CommentInfo> findChildCommentsByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.user.dto.MyCommentSummary(
                c.id,
                c.content,
                c.createdAt,
                p.id,
                p.title,
                p.category.code,
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
            @Param("cursorCommentId") Long cursorCommentId,
            Pageable pageable
    );
}
