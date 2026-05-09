package com.hogu.am_i_hogu.domain.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_bookmarks")
public class PostBookmark {

    @EmbeddedId
    private PostBookmarkId id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PostBookmark(PostBookmarkId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
}
