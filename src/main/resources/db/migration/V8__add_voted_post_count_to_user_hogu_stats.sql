-- add voted post count to user hogu stats
ALTER TABLE user_hogu_stats
    ADD COLUMN voted_post_count INT NOT NULL DEFAULT 0;
