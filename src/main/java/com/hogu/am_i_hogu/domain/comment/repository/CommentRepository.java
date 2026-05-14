package com.hogu.am_i_hogu.domain.comment.repository;

import com.hogu.am_i_hogu.domain.comment.domain.Comment;
import com.hogu.am_i_hogu.domain.comment.dto.PostCommentCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
