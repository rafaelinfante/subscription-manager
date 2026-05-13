CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    name          VARCHAR(255),
    provider      VARCHAR(20)  NOT NULL,
    provider_id   VARCHAR(255),
    enabled       BIT(1)       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    INDEX idx_users_provider (provider, provider_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT       NOT NULL,
    role    VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE plans (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    code             VARCHAR(50)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    amount_cents     BIGINT       NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    billing_interval VARCHAR(10)  NOT NULL,
    active           BIT(1)       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_plans_code UNIQUE (code),
    INDEX idx_plans_active (active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE subscriptions (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT      NOT NULL,
    plan_id                 BIGINT      NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    payment_method_token    VARCHAR(64) NOT NULL,
    trial_end_date          DATE,
    current_period_start    DATE        NOT NULL,
    current_period_end      DATE        NOT NULL,
    next_billing_date       DATE        NOT NULL,
    cancel_at_period_end    BIT(1)      NOT NULL,
    canceled_at             DATETIME(6),
    failed_payment_attempts INT         NOT NULL,
    next_retry_at           DATETIME(6),
    version                 BIGINT      NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    INDEX idx_subscriptions_user (user_id),
    INDEX idx_subscriptions_status (status),
    INDEX idx_subscriptions_next_billing (next_billing_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE invoices (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    subscription_id BIGINT      NOT NULL,
    number          VARCHAR(40) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    amount_cents    BIGINT      NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    period_start    DATE        NOT NULL,
    period_end      DATE        NOT NULL,
    attempt_count   INT         NOT NULL,
    next_retry_at   DATETIME(6),
    failure_reason  VARCHAR(255),
    issued_at       DATETIME(6) NOT NULL,
    paid_at         DATETIME(6),
    version         BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_invoices_number UNIQUE (number),
    CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
    INDEX idx_invoices_subscription (subscription_id),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_next_retry (next_retry_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE subscription_events (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    subscription_id BIGINT      NOT NULL,
    type            VARCHAR(30) NOT NULL,
    detail          VARCHAR(500),
    occurred_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscription_events_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
    INDEX idx_subscription_events_subscription (subscription_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    jti        VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked    BIT(1)      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
