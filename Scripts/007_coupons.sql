-- =============================================================================
-- 007_coupons.sql
-- Schema: promotions
-- Covers: coupons, coupon_redemptions, gift_point_policies
--
-- Also applies deferred FK patches to resolve circular dependencies:
--   - ordering.orders.coupon_id   → promotions.coupons(coupon_id)
--   - checkout.checkout_sessions.coupon_id → promotions.coupons(coupon_id)
--
-- Prerequisites: 001_users.sql, 002_catalog.sql, 003_orders.sql
--   must be executed first.
--
-- Design note:
--   used_count is incremented atomically using:
--     UPDATE promotions.coupons
--        SET used_count = used_count + 1,
--            version    = version + 1,
--            updated_at = now()
--      WHERE coupon_id = $1
--        AND version   = $2
--        AND (max_uses IS NULL OR used_count < max_uses)
--        AND is_deleted = FALSE;
--   Zero rows affected → concurrency conflict or usage limit reached.
--
-- Audit columns on every table:
--   created_at, created_by, updated_at, updated_by, version, is_deleted
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS promotions;

-- ===========================================================================
-- promotions.coupons
-- ===========================================================================
CREATE TABLE promotions.coupons (
    coupon_id       UUID            NOT NULL DEFAULT gen_random_uuid(),
    store_id        UUID            NOT NULL,
    code            VARCHAR(50)     NOT NULL,   -- Always stored UPPERCASE
    description     TEXT            NULL,
    discount_type   VARCHAR(10)     NOT NULL,
    discount_value  NUMERIC(14,2)   NOT NULL,
    min_order_amount NUMERIC(14,2)  NOT NULL DEFAULT 0,
    max_uses        INTEGER         NULL,       -- NULL = unlimited
    used_count      INTEGER         NOT NULL DEFAULT 0,
    expires_at      TIMESTAMPTZ     NULL,       -- NULL = never expires
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_coupons PRIMARY KEY (coupon_id),
    CONSTRAINT fk_coupons_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_coupons_discount_type
        CHECK (discount_type IN ('FLAT','PERCENT')),
    CONSTRAINT chk_coupons_discount_value
        CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_used_count
        CHECK (used_count >= 0),
    CONSTRAINT chk_coupons_max_uses
        CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT chk_coupons_usage_limit
        CHECK (max_uses IS NULL OR used_count <= max_uses),
    CONSTRAINT chk_coupons_min_order
        CHECK (min_order_amount >= 0)
);

-- Coupon validation lookup: store + code, active only
CREATE UNIQUE INDEX uq_coupons_store_code
    ON promotions.coupons (store_id, code)
    WHERE is_deleted = FALSE;

-- Active coupon lookup by code (fastest path for checkout validation)
CREATE INDEX idx_prom_coupons_code
    ON promotions.coupons (store_id, code)
    WHERE is_active = TRUE AND is_deleted = FALSE;

-- Expiry sweep job
CREATE INDEX idx_prom_coupons_expires
    ON promotions.coupons (expires_at)
    WHERE is_active = TRUE
      AND expires_at IS NOT NULL
      AND is_deleted = FALSE;

-- ===========================================================================
-- promotions.coupon_redemptions
-- Tracks every application of a coupon to an order.
-- Soft-deleting a redemption row releases the coupon slot on order cancellation.
-- ===========================================================================
CREATE TABLE promotions.coupon_redemptions (
    redemption_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    coupon_id       UUID            NOT NULL,
    member_id       UUID            NULL,
    guest_token     VARCHAR(200)    NULL,
    order_id        UUID            NOT NULL,
    redeemed_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,   -- is_deleted=TRUE releases coupon slot

    CONSTRAINT pk_coupon_redemptions PRIMARY KEY (redemption_id),
    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES promotions.coupons (coupon_id),
    CONSTRAINT fk_coupon_redemptions_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT fk_coupon_redemptions_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id)
);

-- One active redemption per order
CREATE UNIQUE INDEX uq_coupon_redemptions_order
    ON promotions.coupon_redemptions (order_id)
    WHERE is_deleted = FALSE;

-- All redemptions for a coupon (usage tracking)
CREATE INDEX idx_prom_redemptions_coupon
    ON promotions.coupon_redemptions (coupon_id)
    WHERE is_deleted = FALSE;

-- Redemptions by member (history / per-member use limit enforcement)
CREATE INDEX idx_prom_redemptions_member
    ON promotions.coupon_redemptions (member_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- promotions.gift_point_policies
-- One active policy per store at a time.
-- ===========================================================================
CREATE TABLE promotions.gift_point_policies (
    policy_id               UUID            NOT NULL DEFAULT gen_random_uuid(),
    store_id                UUID            NOT NULL,
    points_per_rupee        NUMERIC(8,4)    NOT NULL,
    rupees_per_point        NUMERIC(8,4)    NOT NULL,
    max_redemption_percent  NUMERIC(5,2)    NOT NULL,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_gift_point_policies PRIMARY KEY (policy_id),
    CONSTRAINT fk_gpp_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_gpp_rates
        CHECK (
            points_per_rupee > 0
            AND rupees_per_point > 0
            AND max_redemption_percent > 0
            AND max_redemption_percent <= 100
        )
);

-- One active gift-point policy per store
CREATE UNIQUE INDEX uq_gpp_active_store
    ON promotions.gift_point_policies (store_id)
    WHERE is_active = TRUE AND is_deleted = FALSE;

-- ===========================================================================
-- DEFERRED FK PATCHES
-- These FKs could not be created earlier because promotions.coupons did not
-- yet exist when ordering and checkout schemas were created.
-- ===========================================================================

-- ordering.orders → promotions.coupons
ALTER TABLE ordering.orders
    ADD CONSTRAINT fk_orders_coupon
        FOREIGN KEY (coupon_id) REFERENCES promotions.coupons (coupon_id);

-- checkout.checkout_sessions → promotions.coupons
ALTER TABLE checkout.checkout_sessions
    ADD CONSTRAINT fk_checkout_sessions_coupon
        FOREIGN KEY (coupon_id) REFERENCES promotions.coupons (coupon_id);

-- Supporting indexes for the patched FK columns
CREATE INDEX idx_ord_orders_coupon
    ON ordering.orders (coupon_id)
    WHERE coupon_id IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX idx_chk_sessions_coupon
    ON checkout.checkout_sessions (coupon_id)
    WHERE coupon_id IS NOT NULL AND is_deleted = FALSE;

-- ===========================================================================
-- Apply updated_at trigger to promotions schema tables
-- ===========================================================================
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'promotions'
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
