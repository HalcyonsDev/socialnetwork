-- =========================================
-- Description: Create the strikes table
-- Author: Halcyon
-- Version: V1.0.0
-- =========================================

CREATE TABLE IF NOT EXISTS strikes (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    cause VARCHAR(500) NOT NULL,
    owner_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (target_id) REFERENCES users(id)
)