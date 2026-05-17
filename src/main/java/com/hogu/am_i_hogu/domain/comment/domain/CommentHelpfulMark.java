package com.hogu.am_i_hogu.domain.comment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comment_helpful_marks")
public class CommentHelpfulMark {
    @EmbeddedId
    private CommentHelpfulMarkId id;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
