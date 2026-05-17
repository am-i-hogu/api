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
@Table(name = "post_votes")
public class PostVote {

    @EmbeddedId
    private PostVoteId id;

    @Column(nullable = false, length = 10)
    private String myVote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PostVote(PostVoteId id, String myVote, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.myVote = myVote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateVote(String myVote, LocalDateTime updatedAt) {
        this.myVote = myVote;
        this.updatedAt = updatedAt;
    }
}
