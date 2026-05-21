package com.hogu.am_i_hogu.domain.post.service;

public enum HomePostSortBy {
    LATEST(" ORDER BY hp.created_at DESC, hp.post_id DESC"),
    MOST_VIEWED(" ORDER BY hp.view_count DESC, hp.post_id DESC"),
    MOST_COMMENTED(" ORDER BY hp.comment_count DESC, hp.post_id DESC"),
    MOST_PARTICIPATED(" ORDER BY hp.total_vote_count DESC, hp.post_id DESC");

    private final String orderBySql;

    HomePostSortBy(String orderBySql) {
        this.orderBySql = orderBySql;
    }

    public String orderBySql() {
        return orderBySql;
    }
}
