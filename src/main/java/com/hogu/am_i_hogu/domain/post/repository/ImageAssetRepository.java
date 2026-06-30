package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {
    List<ImageAsset> findByPost_IdOrderBySortOrderAsc(Long postId);

    boolean existsByUrl(String url);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ImageAsset> findAllWithLockByUrlInOrderByUrlAsc(List<String> urls);
}
