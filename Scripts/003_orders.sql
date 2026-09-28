-- =============================================================================
-- 003_orders.sql
-- Schemas: ordering, checkout
-- Covers: orders, order_delivery_addresses, order_lines, return_requests,
--         checkout_sessions, checkout_addresses
--
-- Prerequisites: 001_users.sql, 002_catalog.sql must be executed first.
--   - ordering.orders references identity.members, store.stores
--   - ordering.order_lines references catalogue.books, catalogue.book_formats
--   - checkout.checkout_sessions references cart.carts, identity.members
--   - ordering.orders references promotions.coupons (added via ALTER TABLE
--     in 007_coupons.sql to avoid circular dependency)
--
-- Audit columns on every table:
--   created_at, created_by, updated_at, updated_by, version, is_deleted
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS checkout;

-- ===========================================================================
-- SCHEMA: ordering
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- ordering.orders
-- ---------------------------------------------------------------------------
CREATE TABLE ordering.orders (
    order_id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    member_id               UUID            NULL,
    guest_email             VARCHAR(320)    NULL,
    store_id                UUID            NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT',

    -- Financials (all amounts in the order's currency)
    subtotal                NUMERIC(14,2)   NOT NULL,
    tax_amount              NUMERIC(14,2)   NOT NULL DEFAULT 0,
    shipping_amount         NUMERIC(14,2)   NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(14,2)   NOT NULL DEFAULT 0,
    wallet_debit_amount     NUMERIC(14,2)   NOT NULL DEFAULT 0,
    grand_total             NUMERIC(14,2)   NOT NULL,
    currency                CHAR(3)         NOT NULL DEFAULT 'INR',

    -- Coupon snapshot (FK to promotions.coupons added in 007_coupons.sql)
    coupon_id               UUID            NULL,
    coupon_code_snapshot    VARCHAR(50)     NULL,

    -- Lifecycle timestamps
    placed_at               TIMESTAMPTZ     NULL,
    confirmed_at            TIMESTAMPTZ     NULL,
    cancelled_at            TIMESTAMPTZ     NULL,
    delivered_at            TIMESTAMPTZ     NULL,
    cancellation_reason     TEXT            NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT fk_orders_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_orders_owner
        CHECK (member_id IS NOT NULL OR guest_email IS NOT NULL),
    CONSTRAINT chk_orders_grand_total
        CHECK (grand_total >= 0),
    CONSTRAINT chk_orders_status CHECK (status IN (
        'PENDING_PAYMENT','AWAITING_PAYMENT','CONFIRMED','PROCESSING',
        'DISPATCHED','DELIVERED','CANCELLED',
        'RETURN_REQUESTED','RETURN_APPROVED','RETURN_REJECTED',
        'RETURN_IN_TRANSIT','RETURN_RECEIVED','REFUNDED'
    ))
);

-- Order history per member (most recent first)
CREATE INDEX idx_ord_orders_member
    ON ordering.orders (member_id, placed_at DESC)
    WHERE is_deleted = FALSE;

-- Status-based admin and sweep queries
CREATE INDEX idx_ord_orders_status
    ON ordering.orders (status)
    WHERE is_deleted = FALSE;

-- Store-level order reporting
CREATE INDEX idx_ord_orders_store
    ON ordering.orders (store_id, placed_at DESC)
    WHERE is_deleted = FALSE;

-- Time-range operational queries
CREATE INDEX idx_ord_orders_placed_at
    ON ordering.orders (placed_at DESC)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- ordering.order_delivery_addresses
-- Immutable snapshot of the delivery address at time of order placement.
-- Changing a member's saved address must never mutate this record.
-- ---------------------------------------------------------------------------
CREATE TABLE ordering.order_delivery_addresses (
    delivery_address_id UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL,
    first_name          VARCHAR(100)    NOT NULL,
    last_name           VARCHAR(100)    NOT NULL,
    line1               VARCHAR(250)    NOT NULL,
    line2               VARCHAR(250)    NULL,
    city                VARCHAR(100)    NOT NULL,
    pin_code            VARCHAR(20)     NOT NULL,
    state               VARCHAR(100)    NOT NULL,
    country             CHAR(2)         NOT NULL DEFAULT 'IN',
    email               VARCHAR(320)    NOT NULL,
    phone               VARCHAR(20)     NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_order_delivery_addresses PRIMARY KEY (delivery_address_id),
    CONSTRAINT fk_oda_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id)
);

-- One delivery address per order
CREATE UNIQUE INDEX uq_oda_order
    ON ordering.order_delivery_addresses (order_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- ordering.order_lines
-- Snapshot columns (title_snapshot, author_snapshot, format_snapshot) are
-- intentional point-in-time records — not 3NF violations.
-- ---------------------------------------------------------------------------
CREATE TABLE ordering.order_lines (
    order_line_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL,
    book_id         UUID            NOT NULL,
    book_format_id  UUID            NOT NULL,

    -- Immutable snapshots captured at order placement
    title_snapshot  VARCHAR(500)    NOT NULL,
    author_snapshot VARCHAR(500)    NULL,
    format_snapshot VARCHAR(20)     NOT NULL,

    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(14,2)   NOT NULL,
    subtotal        NUMERIC(14,2)   NOT NULL,   -- unit_price * quantity
    currency        CHAR(3)         NOT NULL DEFAULT 'INR',

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_order_lines PRIMARY KEY (order_line_id),
    CONSTRAINT fk_order_lines_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id),
    CONSTRAINT fk_order_lines_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT fk_order_lines_format
        FOREIGN KEY (book_format_id)
            REFERENCES catalogue.book_formats (book_format_id),
    CONSTRAINT chk_order_lines_quantity
        CHECK (quantity >= 1),
    CONSTRAINT chk_order_lines_price
        CHECK (unit_price > 0),
    CONSTRAINT chk_order_lines_subtotal
        CHECK (subtotal = unit_price * quantity)
);

CREATE INDEX idx_ord_lines_order
    ON ordering.order_lines (order_id)
    WHERE is_deleted = FALSE;

-- Buy Again: find orders that contain a specific book
CREATE INDEX idx_ord_lines_book
    ON ordering.order_lines (book_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- ordering.return_requests
-- ---------------------------------------------------------------------------
CREATE TABLE ordering.return_requests (
    return_request_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED',
    reason              VARCHAR(200)    NOT NULL,
    notes               TEXT            NULL,
    requested_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_return_requests PRIMARY KEY (return_request_id),
    CONSTRAINT fk_return_requests_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id),
    CONSTRAINT chk_return_requests_status
        CHECK (status IN (
            'REQUESTED','APPROVED','REJECTED',
            'IN_TRANSIT','RECEIVED','REFUNDED'
        ))
);

CREATE INDEX idx_ord_return_requests_order
    ON ordering.return_requests (order_id)
    WHERE is_deleted = FALSE;

-- Admin moderation queue
CREATE INDEX idx_ord_return_requests_status
    ON ordering.return_requests (status)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- SCHEMA: checkout
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- checkout.checkout_sessions
-- Orchestration entity — ties cart snapshot to pricing, address, and payment.
-- coupon_id FK added in 007_coupons.sql to avoid circular dependency.
-- ---------------------------------------------------------------------------
CREATE TABLE checkout.checkout_sessions (
    session_id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    cart_id             UUID            NOT NULL,
    member_id           UUID            NULL,
    guest_token         VARCHAR(200)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    expires_at          TIMESTAMPTZ     NOT NULL,

    -- Pricing (populated after pricing step)
    coupon_id           UUID            NULL,
    wallet_debit_amount NUMERIC(14,2)   NOT NULL DEFAULT 0,
    subtotal            NUMERIC(14,2)   NULL,
    tax_amount          NUMERIC(14,2)   NULL,
    shipping_amount     NUMERIC(14,2)   NULL,
    discount_amount     NUMERIC(14,2)   NULL,
    grand_total         NUMERIC(14,2)   NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'INR',

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_checkout_sessions PRIMARY KEY (session_id),
    CONSTRAINT fk_checkout_sessions_cart
        FOREIGN KEY (cart_id) REFERENCES cart.carts (cart_id),
    CONSTRAINT fk_checkout_sessions_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_checkout_sessions_owner
        CHECK (member_id IS NOT NULL OR guest_token IS NOT NULL),
    CONSTRAINT chk_checkout_sessions_status
        CHECK (status IN (
            'CREATED','ADDRESS_SET','PRICING_APPLIED',
            'CONFIRMED','COMPLETED','EXPIRED'
        ))
);

CREATE INDEX idx_chk_sessions_member
    ON checkout.checkout_sessions (member_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_chk_sessions_cart
    ON checkout.checkout_sessions (cart_id)
    WHERE is_deleted = FALSE;

-- Expiry sweep: find sessions that need to be expired
CREATE INDEX idx_chk_sessions_expires
    ON checkout.checkout_sessions (expires_at)
    WHERE status NOT IN ('COMPLETED','EXPIRED') AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- checkout.checkout_addresses
-- Delivery address captured during the checkout flow.
-- ---------------------------------------------------------------------------
CREATE TABLE checkout.checkout_addresses (
    checkout_address_id UUID            NOT NULL DEFAULT gen_random_uuid(),
    session_id          UUID            NOT NULL,
    first_name          VARCHAR(100)    NOT NULL,
    last_name           VARCHAR(100)    NOT NULL,
    line1               VARCHAR(250)    NOT NULL,
    line2               VARCHAR(250)    NULL,
    city                VARCHAR(100)    NOT NULL,
    pin_code            VARCHAR(20)     NOT NULL,
    state               VARCHAR(100)    NOT NULL,
    country             CHAR(2)         NOT NULL DEFAULT 'IN',
    email               VARCHAR(320)    NOT NULL,
    phone               VARCHAR(20)     NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_checkout_addresses PRIMARY KEY (checkout_address_id),
    CONSTRAINT fk_checkout_addresses_session
        FOREIGN KEY (session_id)
            REFERENCES checkout.checkout_sessions (session_id)
);

-- One address per checkout session
CREATE UNIQUE INDEX uq_checkout_address_session
    ON checkout.checkout_addresses (session_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- Apply updated_at trigger to ordering and checkout schema tables
-- ===========================================================================
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname IN ('ordering','checkout')
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
