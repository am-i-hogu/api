package com.hogu.am_i_hogu.domain.comment.domain;

import com.hogu.am_i_hogu.domain.post.domain.Post;
import com.hogu.am_i_hogu.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments")
public class Comment {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_user_id", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false)
    private int depth;

    @Column(length = 300)
    private String content;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Comment(
            Long id,
            Post post,
            User writer,
            Comment parentComment,
            int depth,
            String content,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.post = post;
        this.writer = writer;
        this.parentComment = parentComment;
        this.depth = depth;
        this.content = content;
        this.isDeleted = false;
        this.deletedAt = null;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
}
