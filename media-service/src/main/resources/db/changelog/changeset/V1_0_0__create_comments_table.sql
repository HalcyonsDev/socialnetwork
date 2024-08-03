-- =========================================
-- Description: Create the comments table
-- Author: Halcyon
-- Version: V1.0.0
-- =========================================

CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    created_at TIMESTAMP NOT NULL,
    content VARCHAR(500) NOT NULL,
    author_email VARCHAR(100) NOT NULL,
    post_id BIGINT NOT NULL,
    parent_id BIGINT,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id)
)