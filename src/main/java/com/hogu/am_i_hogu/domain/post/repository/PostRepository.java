package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Transactional
    @Modifying
    @Query("""
            UPDATE Post p
            SET p.viewCount = p.viewCount + 1
            WHERE p.id = :postId
            """)
    int increaseViewCount(@Param("postId") Long postId);

    @Query("""
            SELECT p.viewCount
            FROM Post p
            WHERE p.id = :postId
            """)
    Optional<Integer> findViewCountById(@Param("postId") Long postId);
}
