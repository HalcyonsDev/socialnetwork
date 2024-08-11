-- =========================================
-- Description: Create the posts table
-- Author: Halcyon
-- Version: V1.0.0
-- =========================================

CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    created_at TIMESTAMP NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    owner_email VARCHAR(100) NOT NULL,
    likes_count INT NOT NULL,
    dislikes_count INT NOT NULL
)