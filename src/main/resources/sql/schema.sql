-- Schema for Activity-Server (MariaDB)
-- This file creates tables matching JPA entities under com.teambind.activityserver.domain.board.entity

-- user_board_activities_count
CREATE TABLE IF NOT EXISTS user_board_activities_count
(
    user_id                 VARCHAR(255) NOT NULL,
    article_count           INT          NOT NULL DEFAULT 0,
    commented_article_count INT          NOT NULL DEFAULT 0,
    liked_article_count     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- user_article
CREATE TABLE IF NOT EXISTS user_article
(
    user_id    VARCHAR(255) NOT NULL,
    article_id VARCHAR(255) NOT NULL,
    created_at DATETIME     NULL,
    PRIMARY KEY (user_id, article_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- user_comment
CREATE TABLE IF NOT EXISTS user_comment
(
    user_id    VARCHAR(255) NOT NULL,
    article_id VARCHAR(255) NOT NULL,
    created_at DATETIME     NULL,
    PRIMARY KEY (user_id, article_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- user_like
CREATE TABLE IF NOT EXISTS user_like
(
    user_id    VARCHAR(255) NOT NULL,
    article_id VARCHAR(255) NOT NULL,
    created_at DATETIME     NULL,
    PRIMARY KEY (user_id, article_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
