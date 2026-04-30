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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
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
    private Long sizeBytes;

    @Column(nullable = false)
    private boolean isThumbnail;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ImageAsset(
            Long id,
            User uploadedByUser,
            Post post,
            String url,
            String contentType,
            boolean isThumbnail,
            Integer sortOrder,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.uploadedByUser = uploadedByUser;
        this.post = post;
        this.url = url;
        this.contentType = contentType;
        this.sizeBytes = 0L;
        this.isThumbnail = isThumbnail;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }
}
