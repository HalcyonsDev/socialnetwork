-- =========================================
-- Description: Create the messages table
-- Author: Halcyon
-- Version: V1.0.0
-- =========================================

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    created_at TIMESTAMP NOT NULL,
    content VARCHAR(1000) NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    is_changed BOOLEAN NOT NULL,
    status VARCHAR(10) NOT NULL
)