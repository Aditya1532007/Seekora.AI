-- =====================================================================
--  Seekora - AI Information Platform
--  MySQL schema (MySQL 5.7+ / 8.x)
--  Run:  mysql -u root -p < database/schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS seekora_db;
CREATE DATABASE seekora_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE seekora_db;

-- ---------------------------------------------------------------------
-- plans : freemium / premium pricing tiers shown on the pricing section
-- ---------------------------------------------------------------------
CREATE TABLE plans (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(30)    NOT NULL UNIQUE,
    name               VARCHAR(60)    NOT NULL,
    price_monthly      DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    price_yearly       DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    daily_search_limit INT            NOT NULL DEFAULT 10,   -- -1 = unlimited
    is_featured        TINYINT(1)     NOT NULL DEFAULT 0,
    sort_order         INT            NOT NULL DEFAULT 0,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE plan_features (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    plan_id     INT          NOT NULL,
    feature     VARCHAR(160) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_plan_features_plan FOREIGN KEY (plan_id)
        REFERENCES plans(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- users : accounts created from the Sign Up / Log In modal
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(120)  NOT NULL,
    email         VARCHAR(160)  NOT NULL UNIQUE,
    password_hash VARCHAR(100)  NOT NULL,             -- BCrypt
    role          ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    plan_id       INT           NOT NULL,
    status        ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME      NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- subscriptions : history of plan purchases / upgrades
-- ---------------------------------------------------------------------
CREATE TABLE subscriptions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    plan_id       INT           NOT NULL,
    billing_cycle ENUM('MONTHLY','YEARLY') NOT NULL DEFAULT 'MONTHLY',
    amount        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status        ENUM('ACTIVE','CANCELLED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    started_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    DATETIME      NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_subs_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
    INDEX idx_subs_user (user_id, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- knowledge_base : topics the AI engine answers from (demo search)
-- ---------------------------------------------------------------------
CREATE TABLE knowledge_base (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic      VARCHAR(160)  NOT NULL,
    keywords   VARCHAR(255)  NOT NULL,           -- comma separated
    summary    TEXT          NOT NULL,
    accuracy   INT           NOT NULL DEFAULT 95,
    is_active  TINYINT(1)    NOT NULL DEFAULT 1,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FULLTEXT KEY ft_kb (topic, keywords, summary)
) ENGINE=InnoDB;

CREATE TABLE knowledge_sources (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_id BIGINT       NOT NULL,
    title        VARCHAR(160) NOT NULL,
    url          VARCHAR(400) NOT NULL,
    source_type  ENUM('ARTICLE','PAPER','IMAGE','VIDEO') NOT NULL DEFAULT 'ARTICLE',
    CONSTRAINT fk_ks_kb FOREIGN KEY (knowledge_id)
        REFERENCES knowledge_base(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- search_queries : every search (guest or logged-in) = search history
-- ---------------------------------------------------------------------
CREATE TABLE search_queries (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NULL,
    session_key      VARCHAR(64)  NULL,
    query_text       VARCHAR(500) NOT NULL,
    answer_text      TEXT         NOT NULL,
    knowledge_id     BIGINT       NULL,
    accuracy         INT          NOT NULL DEFAULT 95,
    response_time_ms INT          NOT NULL DEFAULT 0,
    source_count     INT          NOT NULL DEFAULT 0,
    ip_address       VARCHAR(45)  NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sq_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_sq_kb   FOREIGN KEY (knowledge_id) REFERENCES knowledge_base(id) ON DELETE SET NULL,
    INDEX idx_sq_user_date (user_id, created_at),
    INDEX idx_sq_created (created_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- chat : the floating AI assistant widget
-- ---------------------------------------------------------------------
CREATE TABLE chat_sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NULL,
    session_key VARCHAR(64) NOT NULL UNIQUE,
    started_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE chat_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_session_id BIGINT      NOT NULL,
    sender          ENUM('USER','BOT') NOT NULL,
    message         TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cm_session FOREIGN KEY (chat_session_id)
        REFERENCES chat_sessions(id) ON DELETE CASCADE,
    INDEX idx_cm_session (chat_session_id, created_at)
) ENGINE=InnoDB;

-- chat_intents : keyword driven answers for the assistant
CREATE TABLE chat_intents (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    intent     VARCHAR(60)  NOT NULL,
    keywords   VARCHAR(255) NOT NULL,
    reply      TEXT         NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- marketing / content tables driving the static sections of the page
-- ---------------------------------------------------------------------
CREATE TABLE newsletter_subscribers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(160) NOT NULL UNIQUE,
    status        ENUM('SUBSCRIBED','UNSUBSCRIBED') NOT NULL DEFAULT 'SUBSCRIBED',
    subscribed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE contact_messages (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    message    TEXT         NOT NULL,
    status     ENUM('NEW','READ','REPLIED') NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE faqs (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    question   VARCHAR(255) NOT NULL,
    answer     TEXT         NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE testimonials (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    author_name  VARCHAR(120) NOT NULL,
    author_title VARCHAR(120) NOT NULL,
    initials     VARCHAR(4)   NOT NULL,
    rating       TINYINT      NOT NULL DEFAULT 5,
    content      TEXT         NOT NULL,
    is_approved  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE features (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    icon        VARCHAR(8)   NOT NULL,
    title       VARCHAR(120) NOT NULL,
    description VARCHAR(400) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE platform_stats (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    label      VARCHAR(80) NOT NULL,
    value      INT         NOT NULL,
    suffix     VARCHAR(8)  NOT NULL DEFAULT '',
    sort_order INT         NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- activity log (login, signup, search, upgrade ...)
-- ---------------------------------------------------------------------
CREATE TABLE activity_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NULL,
    action     VARCHAR(60)  NOT NULL,
    details    VARCHAR(400) NULL,
    ip_address VARCHAR(45)  NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_log_user (user_id, created_at)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- convenience view : searches made per user today (freemium limit check)
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_user_searches_today AS
SELECT u.id AS user_id,
       u.email,
       p.code AS plan_code,
       p.daily_search_limit,
       COUNT(s.id) AS searches_today
FROM users u
JOIN plans p ON p.id = u.plan_id
LEFT JOIN search_queries s
       ON s.user_id = u.id AND DATE(s.created_at) = CURDATE()
GROUP BY u.id, u.email, p.code, p.daily_search_limit;
