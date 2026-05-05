package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {
    List<ImageAsset> findByPost_IdOrderBySortOrderAsc(Long postId);
    void deleteByPost_Id(Long postId);
}
