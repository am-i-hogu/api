-- alter voted_post_count column
ALTER TABLE user_hogu_stats
    ADD COLUMN voted_post_count INT NOT NULL DEFAULT 0;