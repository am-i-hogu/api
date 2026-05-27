package com.hogu.am_i_hogu.domain.post.repository;

import com.hogu.am_i_hogu.domain.post.dto.HomePostCursor;
import com.hogu.am_i_hogu.domain.post.dto.HomePostSummary;
import com.hogu.am_i_hogu.domain.post.service.HomePostSortBy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class HomePostQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public HomePostQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 홈 화면 게시물 목록을 조회한다.
     * 게시물 기본 정보와 작성자, 썸네일, 투표 수, 댓글 수, 북마크 여부를 함께 조회한다.
     */
    public List<HomePostSummary> findHomePosts(
            Long viewerUserId,
            String keyword,
            List<String> categoryCodes,
            HomePostSortBy sortBy,
            HomePostCursor cursor,
            int limit
    ) {
        MapSqlParameterSource params = createBaseParams(viewerUserId, keyword, categoryCodes);
        params.addValue("limit", limit);
        addCursorParams(params, cursor);

        String sql = """
                SELECT hp.*
                FROM (
                    -- 홈 목록 카드에 필요한 게시물, 작성자, 집계 정보를 한 번에 만든다.
                    SELECT
                        p.id AS post_id,
                        -- 비회원은 항상 false, 회원은 북마크 row 존재 여부로 계산한다.
                        CASE
                            WHEN :viewerUserId IS NULL THEN FALSE
                            ELSE EXISTS (
                                SELECT 1
                                FROM post_bookmarks pb
                                WHERE pb.post_id = p.id
                                  AND pb.user_id = :viewerUserId
                            )
                        -- CASE 결과를 is_bookmarked로 반환
                        END AS is_bookmarked,
                        p.category_code,
                        p.title,
                        p.created_at,
                        p.view_count,
                        p.content,
                        -- 게시물 이미지 중 썸네일로 지정된 이미지 URL만 홈 카드에 노출한다.
                        thumbnail.url AS thumbnail_url,
                        -- 투표/댓글이 없는 게시물은 집계 서브쿼리 결과가 없으므로 0으로 보정한다.
                        COALESCE(votes.total_vote_count, 0) AS total_vote_count,
                        COALESCE(comments.comment_count, 0) AS comment_count,
                        u.nickname AS writer_nickname,
                        u.profile_image_url AS writer_profile_image_url
                    FROM posts p
                    JOIN users u ON u.id = p.writer_user_id
                    LEFT JOIN image_assets thumbnail
                        ON thumbnail.post_id = p.id
                       AND thumbnail.is_thumbnail = TRUE
                    -- NONE은 투표 취소 상태이므로 참여 수에서 제외한다.
                    LEFT JOIN (
                        SELECT post_id, COUNT(*) AS total_vote_count
                        FROM post_votes
                        WHERE my_vote IN ('HOGU', 'NOT_HOGU')
                        GROUP BY post_id
                    ) votes ON votes.post_id = p.id
                    -- 삭제되지 않은 댓글만 홈 목록 댓글 수로 계산한다.
                    LEFT JOIN (
                        SELECT post_id, COUNT(*) AS comment_count
                        FROM comments
                        WHERE is_deleted = FALSE
                        GROUP BY post_id
                    ) comments ON comments.post_id = p.id
                    -- soft delete된 게시물은 홈 목록에 노출하지 않는다.
                    WHERE p.is_deleted = FALSE
                ) hp
                -- keyword가 없으면 전체 조회, 있으면 제목 포함 검색을 수행한다.
                WHERE (:keyword IS NULL OR hp.title LIKE CONCAT('%', :keyword, '%'))
                """;

        sql += categoryFilterSql(categoryCodes);
        sql += cursorFilterSql(sortBy, cursor);
        sql += sortBy.orderBySql();
        sql += " LIMIT :limit";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new HomePostSummary(
                rs.getLong("post_id"),
                rs.getBoolean("is_bookmarked"),
                rs.getString("category_code"),
                rs.getString("title"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getInt("view_count"),
                rs.getString("content"),
                rs.getString("thumbnail_url"),
                rs.getLong("total_vote_count"),
                rs.getLong("comment_count"),
                rs.getString("writer_nickname"),
                rs.getString("writer_profile_image_url")
        ));
    }

    /**
     * 카테고리 필터가 적용된 홈 목록의 전체 게시물 수를 조회한다.
     * 목록 카드에 필요한 조인과 집계는 제외하고, 필터 조건에 맞는 게시물 수만 계산한다.
     */
    public long countHomePosts(String keyword, List<String> categoryCodes) {
        MapSqlParameterSource params = createBaseParams(null, keyword, categoryCodes);
        String sql = """
                -- 카테고리 필터 시 totalPostCount로 내려줄 게시물 개수를 계산한다.
                SELECT COUNT(*)
                FROM posts p
                -- 삭제된 게시물은 홈 목록과 totalPostCount 모두에서 제외한다.
                WHERE p.is_deleted = FALSE
                  -- keyword가 없으면 전체 조회, 있으면 제목 포함 검색을 수행한다.
                  AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%'))
                """;
        sql += categoryFilterSql(categoryCodes).replace("hp.category_code", "p.category_code");

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    /**
     * 동적 SQL에서 공통으로 사용하는 named parameter를 생성한다.
     * 카테고리 조건이 없을 때는 IN 절 파라미터를 추가하지 않는다.
     */
    private MapSqlParameterSource createBaseParams(Long viewerUserId, String keyword, List<String> categoryCodes) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("viewerUserId", viewerUserId)
                .addValue("keyword", keyword);
        if (!categoryCodes.isEmpty()) {
            params.addValue("categoryCodes", categoryCodes);
        }
        return params;
    }

    /**
     * 카테고리 필터가 있을 때만 IN 절 SQL을 반환한다.
     * 빈 목록이면 전체 카테고리를 조회해야 하므로 아무 조건도 추가하지 않는다.
     */
    private String categoryFilterSql(List<String> categoryCodes) {
        if (categoryCodes.isEmpty()) {
            return "";
        }
        return " AND hp.category_code IN (:categoryCodes)\n";
    }

    /**
     * cursor 기반 페이지네이션에 필요한 기준값을 SQL 파라미터에 추가한다.
     * 정렬 기준에 따라 필요한 값이 다르지만, cursor가 있으면 모든 기준값을 함께 등록한다.
     */
    private void addCursorParams(MapSqlParameterSource params, HomePostCursor cursor) {
        if (cursor == null) {
            return;
        }
        params.addValue("cursorPostId", cursor.postId());
        params.addValue("cursorCreatedAt", Timestamp.valueOf(cursor.createdAt()));
        params.addValue("cursorViewCount", cursor.viewCount());
        params.addValue("cursorCommentCount", cursor.commentCount());
        params.addValue("cursorTotalVoteCount", cursor.totalVoteCount());
    }

    /**
     * 현재 정렬 기준과 cursor를 이용해 다음 페이지 조건 SQL을 만든다.
     * 모든 정렬은 동점 상황에서 postId 내림차순으로 이어지도록 보조 조건을 둔다.
     */
    private String cursorFilterSql(HomePostSortBy sortBy, HomePostCursor cursor) {
        if (cursor == null) {
            return "";
        }
        return switch (sortBy) {
            case LATEST -> """
                     -- 최신순: 이전 페이지 마지막 게시물보다 오래된 게시물을 조회한다.
                     AND (
                         hp.created_at < :cursorCreatedAt
                         OR (hp.created_at = :cursorCreatedAt AND hp.post_id < :cursorPostId)
                     )
                    """;
            case MOST_VIEWED -> """
                     -- 조회수순: 조회수가 더 낮거나, 동점이면 postId가 더 작은 게시물을 조회한다.
                     AND (
                         hp.view_count < :cursorViewCount
                         OR (hp.view_count = :cursorViewCount AND hp.post_id < :cursorPostId)
                     )
                    """;
            case MOST_COMMENTED -> """
                     -- 댓글수순: 댓글 수가 더 적거나, 동점이면 postId가 더 작은 게시물을 조회한다.
                     AND (
                         hp.comment_count < :cursorCommentCount
                         OR (hp.comment_count = :cursorCommentCount AND hp.post_id < :cursorPostId)
                     )
                    """;
            case MOST_PARTICIPATED -> """
                     -- 참여수순: 투표 참여 수가 더 적거나, 동점이면 postId가 더 작은 게시물을 조회한다.
                     AND (
                         hp.total_vote_count < :cursorTotalVoteCount
                         OR (hp.total_vote_count = :cursorTotalVoteCount AND hp.post_id < :cursorPostId)
                     )
                    """;
        };
    }
}
