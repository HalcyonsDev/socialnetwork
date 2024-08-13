-- =========================================
-- Description: Create the users table
-- Author: Halcyon
-- Version: V1.0.0
-- =========================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    email VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    about VARCHAR(500),
    password VARCHAR(500),
    avatar_path VARCHAR(500),
    is_verified BOOLEAN NOT NULL,
    is_banned BOOLEAN NOT NULL,
    is_using_2fa BOOLEAN NOT NULL,
    auth_provider VARCHAR(20) NOT NULL,
    secret VARCHAR(300)
)