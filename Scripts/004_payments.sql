-- =============================================================================
-- 004_payments.sql
-- Schema: payment
-- Covers: payment_transactions, payment_attempts, refunds
--
-- Prerequisites: 001_users.sql, 003_orders.sql must be executed first.
--   - payment.payment_transactions references ordering.orders
--   - payment.refunds references payment.payment_transactions, ordering.orders
--
-- Security note:
--   Raw card numbers are NEVER stored.  Only masked_card_number (e.g. "**** 4242")
--   is persisted post-tokenisation.  card_expiry_month / card_expiry_year are
--   retained for display purposes only; CVV is never stored at any point.
--
-- Audit columns on every table:
--   created_at, created_by, updated_at, updated_by, version, is_deleted
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS payment;

-- ===========================================================================
-- payment.payment_transactions
-- ===========================================================================
CREATE TABLE payment.payment_transactions (
    transaction_id      UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL,

    -- Gateway reference (opaque external IDs)
    gateway_id          VARCHAR(200)    NULL,
    gateway_name        VARCHAR(100)    NULL,

    -- Method & amount
    payment_method      VARCHAR(20)     NOT NULL,
    amount              NUMERIC(14,2)   NOT NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'INR',

    -- Status lifecycle
    status              VARCHAR(20)     NOT NULL DEFAULT 'INITIATED',

    -- Masked card details (PCI-safe — raw number never stored)
    masked_card_number  VARCHAR(20)     NULL,
    cardholder_name     VARCHAR(200)    NULL,
    card_expiry_month   SMALLINT        NULL,
    card_expiry_year    SMALLINT        NULL,

    -- UPI identifier (masked VPA)
    upi_id              VARCHAR(100)    NULL,

    -- Lifecycle timestamps
    confirmed_at        TIMESTAMPTZ     NULL,
    failed_at           TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_payment_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_payment_transactions_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id),
    CONSTRAINT chk_payment_method
        CHECK (payment_method IN ('CREDIT_CARD','DEBIT_CARD','UPI','WALLET')),
    CONSTRAINT chk_payment_amount
        CHECK (amount > 0),
    CONSTRAINT chk_payment_status
        CHECK (status IN (
            'INITIATED','PENDING','CONFIRMED','FAILED','TIMED_OUT',
            'REFUND_PENDING','REFUNDED','REFUND_FAILED'
        )),
    CONSTRAINT chk_card_expiry_month
        CHECK (card_expiry_month IS NULL
               OR (card_expiry_month BETWEEN 1 AND 12))
);

-- Transactions for a given order
CREATE INDEX idx_pay_txn_order
    ON payment.payment_transactions (order_id)
    WHERE is_deleted = FALSE;

-- Sweep for PENDING / TIMED_OUT transactions awaiting resolution
CREATE INDEX idx_pay_txn_status
    ON payment.payment_transactions (status)
    WHERE is_deleted = FALSE;

-- Gateway webhook reconciliation: look up by gateway's own reference
CREATE INDEX idx_pay_txn_gateway
    ON payment.payment_transactions (gateway_id)
    WHERE gateway_id IS NOT NULL AND is_deleted = FALSE;

-- ===========================================================================
-- payment.payment_attempts
-- Records each individual attempt to charge the gateway, including retries.
-- ===========================================================================
CREATE TABLE payment.payment_attempts (
    attempt_id      UUID            NOT NULL DEFAULT gen_random_uuid(),
    transaction_id  UUID            NOT NULL,
    attempted_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    gateway_status  VARCHAR(100)    NOT NULL,
    failure_reason  TEXT            NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_payment_attempts PRIMARY KEY (attempt_id),
    CONSTRAINT fk_payment_attempts_transaction
        FOREIGN KEY (transaction_id)
            REFERENCES payment.payment_transactions (transaction_id)
);

-- All attempts for a transaction, most recent first
CREATE INDEX idx_pay_attempts_txn
    ON payment.payment_attempts (transaction_id, attempted_at DESC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- payment.refunds
-- order_id is denormalised for query efficiency; source of truth is
-- transaction_id → order_id join.
-- ===========================================================================
CREATE TABLE payment.refunds (
    refund_id           UUID            NOT NULL DEFAULT gen_random_uuid(),
    transaction_id      UUID            NOT NULL,
    order_id            UUID            NOT NULL,
    amount              NUMERIC(14,2)   NOT NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'INR',
    reason              VARCHAR(200)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'INITIATED',
    gateway_refund_id   VARCHAR(200)    NULL,
    initiated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_refunds PRIMARY KEY (refund_id),
    CONSTRAINT fk_refunds_transaction
        FOREIGN KEY (transaction_id)
            REFERENCES payment.payment_transactions (transaction_id),
    CONSTRAINT fk_refunds_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id),
    CONSTRAINT chk_refunds_amount
        CHECK (amount > 0),
    CONSTRAINT chk_refunds_status
        CHECK (status IN ('INITIATED','PROCESSING','COMPLETED','FAILED'))
);

-- Refunds for a specific transaction
CREATE INDEX idx_pay_refunds_txn
    ON payment.refunds (transaction_id)
    WHERE is_deleted = FALSE;

-- Refunds for a specific order (used by order detail and notification queries)
CREATE INDEX idx_pay_refunds_order
    ON payment.refunds (order_id)
    WHERE is_deleted = FALSE;

-- Pending refund processing sweep
CREATE INDEX idx_pay_refunds_status
    ON payment.refunds (status)
    WHERE is_deleted = FALSE;

-- Gateway reconciliation via gateway's own refund reference
CREATE INDEX idx_pay_refunds_gateway
    ON payment.refunds (gateway_refund_id)
    WHERE gateway_refund_id IS NOT NULL AND is_deleted = FALSE;

-- ===========================================================================
-- Apply updated_at trigger to payment schema tables
-- ===========================================================================
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'payment'
    LOOP
        EXECUTE format(
            'CREATE OR REPLACE TRIGGER trg_%s_%s_updated_at
             BEFORE UPDATE ON %I.%I
             FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            t.schemaname, t.tablename,
            t.schemaname, t.tablename
        );
    END LOOP;
END;
$$;
