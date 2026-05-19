package com.hogu.am_i_hogu.domain.comment.repository;

import com.hogu.am_i_hogu.domain.comment.domain.CommentHelpfulMark;
import com.hogu.am_i_hogu.domain.comment.domain.CommentHelpfulMarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentHelpfulMarkRepository extends JpaRepository<CommentHelpfulMark, CommentHelpfulMarkId> {
    long countById_CommentId(Long commentId);
}
