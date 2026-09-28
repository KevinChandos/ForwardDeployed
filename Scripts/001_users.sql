-- =============================================================================
-- 001_users.sql
-- Schemas: identity, store, discovery, cart, wallet, notification, outbox
-- Covers: members, credentials, addresses, sessions, roles, author follows,
--         stores, store policies, tax rules, delivery thresholds,
--         recommendation profiles, recommended books, featured lists/entries,
--         carts, cart items, wishlists, wishlist items,
--         wallet accounts, wallet transactions,
--         notification templates, notification events,
--         and the transactional outbox table.
--
-- Audit columns used on every table:
--   created_at  TIMESTAMPTZ  NOT NULL  DEFAULT now()
--   created_by  UUID         NULL
--   updated_at  TIMESTAMPTZ  NOT NULL  DEFAULT now()
--   updated_by  UUID         NULL
--   version     INTEGER      NOT NULL  DEFAULT 1
--   is_deleted  BOOLEAN      NOT NULL  DEFAULT FALSE
--
-- Soft-delete:  is_deleted = TRUE  means the row is logically removed.
-- Optimistic locking: application must increment `version` on every UPDATE
--   and assert the prior value:
--     UPDATE t SET ..., version = version + 1 WHERE id = $1 AND version = $2
--
-- Execution order matters: identity tables first (other schemas reference them).
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Extensions
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()

-- ---------------------------------------------------------------------------
-- Schemas
-- ---------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS store;
CREATE SCHEMA IF NOT EXISTS discovery;
CREATE SCHEMA IF NOT EXISTS cart;
CREATE SCHEMA IF NOT EXISTS wallet;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS outbox;

-- ===========================================================================
-- SCHEMA: identity
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- identity.members
-- ---------------------------------------------------------------------------
CREATE TABLE identity.members (
    member_id       UUID            NOT NULL DEFAULT gen_random_uuid(),
    display_name    VARCHAR(150)    NOT NULL,
    email           VARCHAR(320)    NULL,
    phone_number    VARCHAR(20)     NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Audit
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      UUID            NULL,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by      UUID            NULL,
    version         INTEGER         NOT NULL DEFAULT 1,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_members PRIMARY KEY (member_id),
    CONSTRAINT chk_members_status
        CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')),
    CONSTRAINT chk_members_contact
        CHECK (email IS NOT NULL OR phone_number IS NOT NULL)
);

-- Partial unique indexes enforce uniqueness only among active rows
CREATE UNIQUE INDEX uq_members_email
    ON identity.members (email)
    WHERE email IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX uq_members_phone
    ON identity.members (phone_number)
    WHERE phone_number IS NOT NULL AND is_deleted = FALSE;

-- Supporting indexes
CREATE INDEX idx_identity_members_status
    ON identity.members (status)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- identity.credentials
-- ---------------------------------------------------------------------------
CREATE TABLE identity.credentials (
    credential_id       UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id           UUID            NOT NULL,
    channel             VARCHAR(10)     NOT NULL,
    hashed_secret       VARCHAR(255)    NOT NULL,
    reset_token         VARCHAR(255)    NULL,
    reset_expires_at    TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_credentials PRIMARY KEY (credential_id),
    CONSTRAINT fk_credentials_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_credentials_channel
        CHECK (channel IN ('EMAIL','PHONE'))
);

CREATE UNIQUE INDEX uq_credentials_member_channel
    ON identity.credentials (member_id, channel)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_identity_credentials_member
    ON identity.credentials (member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_identity_credentials_reset_token
    ON identity.credentials (reset_token)
    WHERE reset_token IS NOT NULL AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- identity.member_addresses
-- ---------------------------------------------------------------------------
CREATE TABLE identity.member_addresses (
    address_id  UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id   UUID            NOT NULL,
    label       VARCHAR(100)    NULL,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    line1       VARCHAR(250)    NOT NULL,
    line2       VARCHAR(250)    NULL,
    city        VARCHAR(100)    NOT NULL,
    pin_code    VARCHAR(20)     NOT NULL,
    state       VARCHAR(100)    NOT NULL,
    country     CHAR(2)         NOT NULL DEFAULT 'IN',
    is_default  BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_member_addresses PRIMARY KEY (address_id),
    CONSTRAINT fk_addresses_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE INDEX idx_identity_addresses_member
    ON identity.member_addresses (member_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- identity.sessions
-- ---------------------------------------------------------------------------
CREATE TABLE identity.sessions (
    session_id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id           UUID            NOT NULL,
    access_token_hash   VARCHAR(255)    NOT NULL,
    refresh_token_hash  VARCHAR(255)    NOT NULL,
    expires_at          TIMESTAMPTZ     NOT NULL,
    device_info         VARCHAR(500)    NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,   -- is_deleted=TRUE means logged out

    CONSTRAINT pk_sessions PRIMARY KEY (session_id),
    CONSTRAINT fk_sessions_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE INDEX idx_identity_sessions_member
    ON identity.sessions (member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_identity_sessions_expires
    ON identity.sessions (expires_at)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- identity.member_roles
-- ---------------------------------------------------------------------------
CREATE TABLE identity.member_roles (
    member_role_id  UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id       UUID            NOT NULL,
    role_name       VARCHAR(50)     NOT NULL,
    granted_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,   -- is_deleted=TRUE means role revoked

    CONSTRAINT pk_member_roles PRIMARY KEY (member_role_id),
    CONSTRAINT fk_member_roles_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_member_roles_name
        CHECK (role_name IN (
            'GUEST','REGISTERED_USER','STORE_ADMIN',
            'CATALOGUE_MANAGER','PLATFORM_ADMIN'
        ))
);

CREATE UNIQUE INDEX uq_member_roles_member_role
    ON identity.member_roles (member_id, role_name)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: store
-- (Defined here because catalogue.book_prices and discovery.featured_lists
--  reference store.stores; catalogue schema is in 002_catalog.sql)
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- store.stores
-- ---------------------------------------------------------------------------
CREATE TABLE store.stores (
    store_id        UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(200)    NOT NULL,
    slug            VARCHAR(200)    NOT NULL,
    region          VARCHAR(100)    NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    owner_member_id UUID            NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_stores PRIMARY KEY (store_id),
    CONSTRAINT fk_stores_owner
        FOREIGN KEY (owner_member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_stores_status
        CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE UNIQUE INDEX uq_stores_slug
    ON store.stores (slug)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_store_stores_status
    ON store.stores (status)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- store.store_policies
-- ---------------------------------------------------------------------------
CREATE TABLE store.store_policies (
    policy_id                   UUID            NOT NULL DEFAULT gen_random_uuid(),
    store_id                    UUID            NOT NULL,
    return_window_days          INTEGER         NOT NULL DEFAULT 7,
    free_delivery_threshold     NUMERIC(14,2)   NULL,
    currency                    CHAR(3)         NOT NULL DEFAULT 'INR',
    is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_store_policies PRIMARY KEY (policy_id),
    CONSTRAINT fk_store_policies_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_store_policies_return_days
        CHECK (return_window_days > 0)
);

-- Only one active policy per store at a time
CREATE UNIQUE INDEX uq_store_policies_active
    ON store.store_policies (store_id)
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE INDEX idx_store_policies_store
    ON store.store_policies (store_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- store.tax_rules
-- ---------------------------------------------------------------------------
CREATE TABLE store.tax_rules (
    tax_rule_id     UUID            NOT NULL DEFAULT gen_random_uuid(),
    store_id        UUID            NOT NULL,
    tax_category    VARCHAR(100)    NOT NULL,
    rate_percent    NUMERIC(5,2)    NOT NULL,
    effective_from  DATE            NOT NULL,
    effective_to    DATE            NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_tax_rules PRIMARY KEY (tax_rule_id),
    CONSTRAINT fk_tax_rules_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_tax_rules_rate
        CHECK (rate_percent >= 0 AND rate_percent <= 100),
    CONSTRAINT chk_tax_rules_dates
        CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_store_tax_rules_store
    ON store.tax_rules (store_id, effective_from DESC)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- store.delivery_thresholds
-- ---------------------------------------------------------------------------
CREATE TABLE store.delivery_thresholds (
    threshold_id        UUID            NOT NULL DEFAULT gen_random_uuid(),
    store_id            UUID            NOT NULL,
    min_order_amount    NUMERIC(14,2)   NOT NULL,
    shipping_cost       NUMERIC(14,2)   NOT NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'INR',

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_delivery_thresholds PRIMARY KEY (threshold_id),
    CONSTRAINT fk_delivery_thresholds_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_delivery_thresholds_amounts
        CHECK (min_order_amount >= 0 AND shipping_cost >= 0)
);

CREATE INDEX idx_store_thresholds_store
    ON store.delivery_thresholds (store_id, min_order_amount)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: discovery
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- discovery.recommendation_profiles
-- ---------------------------------------------------------------------------
CREATE TABLE discovery.recommendation_profiles (
    profile_id      UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id       UUID            NULL,
    guest_token     VARCHAR(200)    NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_recommendation_profiles PRIMARY KEY (profile_id),
    CONSTRAINT fk_rp_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_rp_owner
        CHECK (member_id IS NOT NULL OR guest_token IS NOT NULL)
);

CREATE UNIQUE INDEX uq_rp_member
    ON discovery.recommendation_profiles (member_id)
    WHERE member_id IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX uq_rp_guest
    ON discovery.recommendation_profiles (guest_token)
    WHERE guest_token IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX idx_disc_rp_member
    ON discovery.recommendation_profiles (member_id)
    WHERE member_id IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX idx_disc_rp_guest
    ON discovery.recommendation_profiles (guest_token)
    WHERE guest_token IS NOT NULL AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- discovery.featured_lists
-- ---------------------------------------------------------------------------
CREATE TABLE discovery.featured_lists (
    list_id         UUID            NOT NULL DEFAULT gen_random_uuid(),
    store_id        UUID            NOT NULL,
    list_type       VARCHAR(30)     NOT NULL,
    effective_from  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    effective_to    TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_featured_lists PRIMARY KEY (list_id),
    CONSTRAINT fk_featured_lists_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_featured_lists_type
        CHECK (list_type IN ('BESTSELLER','NEW_LAUNCH','RECOMMENDED'))
);

CREATE INDEX idx_disc_fl_store_type
    ON discovery.featured_lists (store_id, list_type, effective_from DESC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: cart
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- cart.carts
-- ---------------------------------------------------------------------------
CREATE TABLE cart.carts (
    cart_id     UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id   UUID            NULL,
    guest_token VARCHAR(200)    NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_carts PRIMARY KEY (cart_id),
    CONSTRAINT fk_carts_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_carts_owner
        CHECK (member_id IS NOT NULL OR guest_token IS NOT NULL),
    CONSTRAINT chk_carts_status
        CHECK (status IN ('ACTIVE','CHECKING_OUT','CONVERTED','MERGED'))
);

CREATE UNIQUE INDEX uq_carts_active_member
    ON cart.carts (member_id)
    WHERE status = 'ACTIVE' AND member_id IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX uq_carts_active_guest
    ON cart.carts (guest_token)
    WHERE status = 'ACTIVE' AND guest_token IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX idx_cart_carts_member
    ON cart.carts (member_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_cart_carts_guest
    ON cart.carts (guest_token)
    WHERE guest_token IS NOT NULL AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- cart.wishlists
-- ---------------------------------------------------------------------------
CREATE TABLE cart.wishlists (
    wishlist_id UUID    NOT NULL DEFAULT gen_random_uuid(),
    member_id   UUID    NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_wishlists PRIMARY KEY (wishlist_id),
    CONSTRAINT fk_wishlists_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_wishlists_member
    ON cart.wishlists (member_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: wallet
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- wallet.wallet_accounts
-- ---------------------------------------------------------------------------
CREATE TABLE wallet.wallet_accounts (
    wallet_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id   UUID            NOT NULL,
    balance     NUMERIC(14,2)   NOT NULL DEFAULT 0.00,
    currency    CHAR(3)         NOT NULL DEFAULT 'INR',
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_wallet_accounts PRIMARY KEY (wallet_id),
    CONSTRAINT fk_wallet_accounts_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_wallet_balance
        CHECK (balance >= 0),
    CONSTRAINT chk_wallet_status
        CHECK (status IN ('ACTIVE','FROZEN'))
);

CREATE UNIQUE INDEX uq_wallet_member
    ON wallet.wallet_accounts (member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_wal_accounts_member
    ON wallet.wallet_accounts (member_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- wallet.wallet_transactions
-- ---------------------------------------------------------------------------
CREATE TABLE wallet.wallet_transactions (
    wallet_txn_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    wallet_id       UUID            NOT NULL,
    txn_type        VARCHAR(10)     NOT NULL,
    amount          NUMERIC(14,2)   NOT NULL,
    source          VARCHAR(20)     NOT NULL,
    reference_id    UUID            NULL,
    balance_after   NUMERIC(14,2)   NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_wallet_transactions PRIMARY KEY (wallet_txn_id),
    CONSTRAINT fk_wallet_transactions_account
        FOREIGN KEY (wallet_id) REFERENCES wallet.wallet_accounts (wallet_id),
    CONSTRAINT chk_wallet_txn_type
        CHECK (txn_type IN ('CREDIT','DEBIT')),
    CONSTRAINT chk_wallet_txn_source
        CHECK (source IN ('REFUND','GIFT','REDEMPTION','ADJUSTMENT')),
    CONSTRAINT chk_wallet_txn_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_wal_transactions_wallet
    ON wallet.wallet_transactions (wallet_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_wal_transactions_reference
    ON wallet.wallet_transactions (reference_id)
    WHERE reference_id IS NOT NULL AND is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: notification
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- notification.notification_templates
-- ---------------------------------------------------------------------------
CREATE TABLE notification.notification_templates (
    template_code   VARCHAR(100)    NOT NULL,
    subject         VARCHAR(500)    NULL,
    body_template   TEXT            NOT NULL,
    channel         VARCHAR(10)     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_notification_templates PRIMARY KEY (template_code),
    CONSTRAINT chk_notif_template_channel
        CHECK (channel IN ('EMAIL','SMS','PUSH'))
);

-- ---------------------------------------------------------------------------
-- notification.notification_events
-- ---------------------------------------------------------------------------
CREATE TABLE notification.notification_events (
    notification_id     UUID            NOT NULL DEFAULT gen_random_uuid(),
    recipient_member_id UUID            NULL,
    recipient_email     VARCHAR(320)    NULL,
    channel             VARCHAR(10)     NOT NULL,
    template_code       VARCHAR(100)    NOT NULL,
    payload             JSONB           NOT NULL DEFAULT '{}',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    sent_at             TIMESTAMPTZ     NULL,
    failure_reason      TEXT            NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_notification_events PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_events_member
        FOREIGN KEY (recipient_member_id) REFERENCES identity.members (member_id),
    CONSTRAINT fk_notif_events_template
        FOREIGN KEY (template_code)
            REFERENCES notification.notification_templates (template_code),
    CONSTRAINT chk_notif_event_channel
        CHECK (channel IN ('EMAIL','SMS','PUSH')),
    CONSTRAINT chk_notif_event_status
        CHECK (status IN ('PENDING','SENT','FAILED','BOUNCED'))
);

CREATE INDEX idx_notif_events_member
    ON notification.notification_events (recipient_member_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_notif_events_status
    ON notification.notification_events (status)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: outbox
-- (Transactional outbox — written in same transaction as aggregate mutations)
-- ===========================================================================

CREATE TABLE outbox.domain_event_outbox (
    event_id        UUID            NOT NULL DEFAULT gen_random_uuid(),
    event_type      VARCHAR(200)    NOT NULL,
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    UUID            NOT NULL,
    payload         JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    occurred_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ     NULL,
    retry_count     INTEGER         NOT NULL DEFAULT 0,

    -- Audit (no is_deleted — outbox rows are permanent records)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_domain_event_outbox PRIMARY KEY (event_id),
    CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING','PUBLISHED','FAILED'))
);

-- Relay process polls this index: SKIP LOCKED on PENDING rows ordered by time
CREATE INDEX idx_outbox_status
    ON outbox.domain_event_outbox (status, occurred_at ASC)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_aggregate
    ON outbox.domain_event_outbox (aggregate_type, aggregate_id, occurred_at ASC);

-- ===========================================================================
-- updated_at trigger function (reusable across all schemas)
-- ===========================================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- Apply to every table that has updated_at
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname IN (
            'identity','store','discovery','cart',
            'wallet','notification','outbox'
        )
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
