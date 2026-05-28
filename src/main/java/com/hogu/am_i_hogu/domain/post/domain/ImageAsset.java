package com.hogu.am_i_hogu.domain.post.domain;

import com.hogu.am_i_hogu.domain.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "image_assets")
public class ImageAsset {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false, length = 512, unique = true)
    private String url;

    @Column(nullable = false, length = 64)
    private String contentType;

    @Column(nullable = false)
    @Builder.Default
    private Long sizeBytes = 0L;

    @Column(nullable = false)
    private boolean isThumbnail;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public void attachTo(Post post, boolean isThumbnail, Integer sortOrder) {
        this.post = post;
        this.isThumbnail = isThumbnail;
        this.sortOrder = sortOrder;
    }

    public void detach() {
        this.post = null;
        this.isThumbnail = false;
        this.sortOrder = 0;
    }
}
