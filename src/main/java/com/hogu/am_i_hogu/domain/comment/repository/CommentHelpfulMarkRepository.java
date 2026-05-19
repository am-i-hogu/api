package com.hogu.am_i_hogu.domain.comment.repository;

import com.hogu.am_i_hogu.domain.comment.domain.CommentHelpfulMark;
import com.hogu.am_i_hogu.domain.comment.domain.CommentHelpfulMarkId;
import com.hogu.am_i_hogu.domain.comment.dto.CommentHelpfulCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentHelpfulMarkRepository extends JpaRepository<CommentHelpfulMark, CommentHelpfulMarkId> {
    long countById_CommentId(Long commentId);

    @Query("""
            SELECT new com.hogu.am_i_hogu.domain.comment.dto.CommentHelpfulCount(
                chm.id.commentId,
                COUNT(chm)
            )
            FROM CommentHelpfulMark chm
            WHERE chm.id IN :commendIds
            GROUP BY chm.id.commentId
            """)
    List<CommentHelpfulCount> countCommentHelpfulMarksGroupedByCommentIds(
            @Param("commendIds") List<Long> commentIds
    );

    @Query("""
            SELECT chm.id.commentId
            FROM CommentHelpfulMark chm
            WHERE chm.id.userId = :userId
                AND chm.id.commentId IN :commentIds
            """)
    List<Long> findHelpfulCommentIdsByUserIdAndCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds
    );
}
