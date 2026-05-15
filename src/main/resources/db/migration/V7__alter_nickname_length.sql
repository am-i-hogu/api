-- alter User nickname length
ALTER TABLE users
    MODIFY COLUMN nickname VARCHAR(22) NOT NULL UNIQUE;