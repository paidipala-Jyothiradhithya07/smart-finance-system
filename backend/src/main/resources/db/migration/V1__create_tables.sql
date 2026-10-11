-- =========================================================
-- V1: Core tables for Smart Finance Management System
-- =========================================================

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    currency      VARCHAR(3)   NOT NULL DEFAULT 'INR',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NULL,
    name       VARCHAR(60) NOT NULL,
    type       VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_categories_type CHECK (type IN ('INCOME', 'EXPENSE', 'BOTH'))
);

CREATE TABLE bank_statements (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_hash       VARCHAR(64)  NOT NULL,
    total_rows      INT          NOT NULL DEFAULT 0,
    imported_count  INT          NOT NULL DEFAULT 0,
    duplicate_count INT          NOT NULL DEFAULT 0,
    invalid_count   INT          NOT NULL DEFAULT 0,
    period_start    DATE         NULL,
    period_end      DATE         NULL,
    status          VARCHAR(25)  NOT NULL,
    uploaded_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_statements_user_hash UNIQUE (user_id, file_hash),
    CONSTRAINT fk_statements_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_statements_status CHECK (status IN
        ('PROCESSING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED'))
);

CREATE TABLE transactions (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    bank_statement_id BIGINT        NULL,
    category_id       BIGINT        NOT NULL,
    transaction_date  DATE          NOT NULL,
    description       VARCHAR(500)  NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    type              VARCHAR(10)   NOT NULL,
    balance_after     DECIMAL(15,2) NULL,
    payment_method    VARCHAR(30)   NULL,
    source            VARCHAR(20)   NOT NULL,
    reference_no      VARCHAR(100)  NULL,
    row_sequence      INT           NULL,
    dedup_hash        VARCHAR(64)   NULL,
    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_statement FOREIGN KEY (bank_statement_id)
        REFERENCES bank_statements (id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id)
        REFERENCES categories (id),
    CONSTRAINT uk_tx_user_dedup UNIQUE (user_id, dedup_hash),
    CONSTRAINT chk_tx_amount CHECK (amount > 0),
    CONSTRAINT chk_tx_type   CHECK (type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_tx_source CHECK (source IN ('MANUAL', 'BANK_STATEMENT')),
    INDEX idx_tx_user_date          (user_id, transaction_date),
    INDEX idx_tx_user_type_date     (user_id, type, transaction_date),
    INDEX idx_tx_user_category_date (user_id, category_id, transaction_date),
    INDEX idx_tx_statement          (bank_statement_id)
);

CREATE TABLE budgets (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    category_id  BIGINT        NOT NULL,
    budget_year  INT           NOT NULL,
    budget_month INT           NOT NULL,
    amount       DECIMAL(15,2) NOT NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_budgets_unique UNIQUE (user_id, category_id, budget_year, budget_month),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id)
        REFERENCES categories (id),
    CONSTRAINT chk_budgets_amount CHECK (amount > 0),
    CONSTRAINT chk_budgets_month  CHECK (budget_month BETWEEN 1 AND 12)
);