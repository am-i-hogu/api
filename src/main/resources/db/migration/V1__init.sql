-- User
CREATE TABLE users (
    id BIGINT NOT NULL PRIMARY KEY,
    nickname VARCHAR(20) NOT NULL UNIQUE,
    profile_image_url VARCHAR(512) NULL,
    is_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- Categories
CREATE TABLE categories (
    code VARCHAR(20) NOT NULL PRIMARY KEY,
    display_name VARCHAR(10) NOT NULL
);

-- Hogu Levels
CREATE TABLE hogu_levels (
    code VARCHAR(20) NOT NULL PRIMARY KEY,
    display_name VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    min_hogu_index INT NOT NULL,
    max_hogu_index INT NOT NULL
);

-- Policy Revisions
CREATE TABLE policy_revisions (
    id BIGINT NOT NULL PRIMARY KEY,
    policy_type VARCHAR(20) NOT NULL,
    version VARCHAR(20) NOT NULL,
    html_content TEXT NOT NULL,
    is_current BOOLEAN NOT NULL,
    updated_at DATETIME NOT NULL
);

-- Social Accounts
CREATE TABLE social_accounts (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NULL,
    provider VARCHAR(16) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    linked_at DATETIME NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_social_accounts_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_social_accounts_provider_provider_user_id
        UNIQUE (provider, provider_user_id)

);

-- OAuth Login States
CREATE TABLE oauth_login_states (
    id BIGINT NOT NULL PRIMARY KEY,
    provider VARCHAR(16) NOT NULL,
    state VARCHAR(64) NOT NULL UNIQUE,
    nonce VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL
);

-- User Hogu Stats
CREATE TABLE user_hogu_stats (
    user_id BIGINT NOT NULL PRIMARY KEY,
    hogu_vote_count INT NOT NULL DEFAULT 0,
    total_vote_count INT NOT NULL DEFAULT 0,
    hogu_index INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_user_hogu_stats_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Posts
CREATE TABLE posts (
    id BIGINT NOT NULL PRIMARY KEY,
    writer_user_id BIGINT NOT NULL,
    category_code VARCHAR(20) NOT NULL,
    title VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    view_count INT NOT NULL,
    is_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_posts_writer_user_id
       FOREIGN KEY (writer_user_id) REFERENCES users(id),
    CONSTRAINT fk_posts_category_code
       FOREIGN KEY (category_code) REFERENCES categories(code)
);

-- Register Sessions
CREATE TABLE register_sessions (
    id BIGINT NOT NULL PRIMARY KEY,
    social_account_id BIGINT NOT NULL,
    register_token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_register_sessions_social_account_id
       FOREIGN KEY (social_account_id) REFERENCES social_accounts(id)
);

-- Refresh Token
CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    is_revoked BOOLEAN NOT NULL,
    is_rotated BOOLEAN NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_refresh_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Image Assets
CREATE TABLE image_assets (
    id BIGINT NOT NULL PRIMARY KEY,
    uploaded_by_user_id BIGINT NOT NULL ,
    post_id BIGINT NULL,
    url VARCHAR(512) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    is_thumbnail BOOLEAN NOT NULL,
    sort_order INT NOT NULL,
    thumbnail_post_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN is_thumbnail = TRUE AND post_id IS NOT NULL THEN post_id
            ELSE NULL
        END
    ) STORED,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_image_assets_uploaded_by_user_id
      FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_image_assets_post_id
      FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT uk_image_assets_thumbnail_post_id
      UNIQUE (thumbnail_post_id)
);

-- Comments
CREATE TABLE comments (
    id BIGINT NOT NULL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    writer_user_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    depth INT NOT NULL CHECK (depth IN (0, 1)),
    content VARCHAR(300) NULL,
    is_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_comments_post_id
      FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_comments_writer_user_id
      FOREIGN KEY (writer_user_id) REFERENCES users(id),
    CONSTRAINT fk_comments_parent_comment_id
      FOREIGN KEY (parent_comment_id) REFERENCES comments(id),
    CONSTRAINT check_comments_depth_parent_consistency
      CHECK (
          (depth = 0 AND parent_comment_id IS NULL) OR
          (depth = 1 AND parent_comment_id IS NOT NULL)
      )
);

-- Post Bookmarks
CREATE TABLE post_bookmarks (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_post_bookmarks_post_id
        FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_post_bookmarks_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Post Votes
CREATE TABLE post_votes (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    my_vote VARCHAR(10) NOT NULL DEFAULT 'NONE' CHECK (my_vote IN ('HOGU', 'NOT_HOGU', 'NONE')),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_post_votes_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_post_votes_post_id
        FOREIGN KEY (post_id) REFERENCES posts(id)
);

-- Comment Helpful Marks
CREATE TABLE comment_helpful_marks (
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (user_id, comment_id),
    CONSTRAINT fk_comment_helpful_marks_comment_id
        FOREIGN KEY (comment_id) REFERENCES comments(id),
    CONSTRAINT fk_comment_helpful_marks_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
);