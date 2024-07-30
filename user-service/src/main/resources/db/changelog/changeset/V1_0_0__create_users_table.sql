-- =========================================
-- Description: Create the users table
-- Author: Halcyon
-- Version: V1.0.0
-- =========================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    email VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    about VARCHAR(500) NOT NULL,
    password VARCHAR(500) NOT NULL,
    is_verified BOOLEAN NOT NULL
)