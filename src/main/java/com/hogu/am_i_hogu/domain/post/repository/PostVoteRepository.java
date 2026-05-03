package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.domain.PostVote;
import com.hogu.am_i_hogu.domain.post.domain.PostVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, PostVoteId> {

    @Query("""
            SELECT COUNT(pv)
            FROM PostVote pv
            WHERE pv.id.postId = :postId
              AND pv.myVote = :myVote
            """)
    long countByPostIdAndMyVote(@Param("postId") Long postId, @Param("myVote") String myVote);

    @Query("""
            SELECT pv
            FROM PostVote pv
            WHERE pv.id.postId = :postId
              AND pv.id.userId = :userId
            """)
    // 아직 투표나 투표 취소를 안했을 경우에 NULL일 수 있으므로 Optional로 설정한다.
    Optional<PostVote> findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
