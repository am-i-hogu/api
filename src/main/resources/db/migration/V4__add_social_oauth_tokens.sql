-- Social Oauth Tokens
CREATE TABLE social_oauth_tokens (
    id BIGINT NOT NULL PRIMARY KEY,
    social_account_id BIGINT NOT NULL,
    access_token_encrypted TEXT NULL,
    refresh_token_encrypted TEXT NULL,
    access_token_expires_at DATETIME NULL,
    refresh_token_expires_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_social_oauth_tokens_social_account_id
        FOREIGN KEY (social_account_id) REFERENCES social_accounts(id),
    CONSTRAINT uk_social_oauth_tokens_social_account_id
        UNIQUE (social_account_id)
)