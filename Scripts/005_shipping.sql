-- =============================================================================
-- 005_shipping.sql
-- Schema: shipping
-- Covers: shipments, shipment_events, return_shipments
--
-- Prerequisites: 003_orders.sql must be executed first.
--   - shipping.shipments references ordering.orders
--   - shipping.return_shipments references ordering.return_requests
--
-- Audit columns on every table:
--   created_at, created_by, updated_at, updated_by, version, is_deleted
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS shipping;

-- ===========================================================================
-- shipping.shipments
-- One shipment record per order (physical books only).
-- eBook delivery is out of scope for this schema.
-- ===========================================================================
CREATE TABLE shipping.shipments (
    shipment_id             UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id                UUID            NOT NULL,
    carrier                 VARCHAR(100)    NULL,
    tracking_number         VARCHAR(200)    NULL,
    tracking_url            VARCHAR(2000)   NULL,
    estimated_delivery_date DATE            NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    dispatched_at           TIMESTAMPTZ     NULL,
    delivered_at            TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_shipments PRIMARY KEY (shipment_id),
    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id),
    CONSTRAINT chk_shipments_status
        CHECK (status IN (
            'CREATED','READY_FOR_DISPATCH','DISPATCHED',
            'IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','CANCELLED'
        ))
);

-- One active shipment per order
CREATE UNIQUE INDEX uq_shipments_order
    ON shipping.shipments (order_id)
    WHERE is_deleted = FALSE;

-- Shipment lookup by order
CREATE INDEX idx_ship_shipments_order
    ON shipping.shipments (order_id)
    WHERE is_deleted = FALSE;

-- Dispatch processing sweep
CREATE INDEX idx_ship_shipments_status
    ON shipping.shipments (status)
    WHERE is_deleted = FALSE;

-- Carrier webhook reconciliation via tracking number
CREATE INDEX idx_ship_shipments_tracking
    ON shipping.shipments (tracking_number)
    WHERE tracking_number IS NOT NULL AND is_deleted = FALSE;

-- ===========================================================================
-- shipping.shipment_events
-- Append-only log of carrier scan events for a shipment.
-- Events must be written in chronological order (occurred_at ASC).
-- ===========================================================================
CREATE TABLE shipping.shipment_events (
    event_id        UUID            NOT NULL DEFAULT gen_random_uuid(),
    shipment_id     UUID            NOT NULL,
    event_type      VARCHAR(50)     NOT NULL,
    location        VARCHAR(300)    NULL,
    notes           TEXT            NULL,
    occurred_at     TIMESTAMPTZ     NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_shipment_events PRIMARY KEY (event_id),
    CONSTRAINT fk_shipment_events_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipping.shipments (shipment_id)
);

-- Tracking timeline for a shipment, oldest event first
CREATE INDEX idx_ship_events_shipment
    ON shipping.shipment_events (shipment_id, occurred_at ASC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- shipping.return_shipments
-- One return shipment per return request.
-- Created when a ReturnRequest is approved.
-- ===========================================================================
CREATE TABLE shipping.return_shipments (
    return_shipment_id  UUID            NOT NULL DEFAULT gen_random_uuid(),
    return_request_id   UUID            NOT NULL,
    carrier             VARCHAR(100)    NULL,
    tracking_number     VARCHAR(200)    NULL,
    tracking_url        VARCHAR(2000)   NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    picked_up_at        TIMESTAMPTZ     NULL,
    received_at         TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_return_shipments PRIMARY KEY (return_shipment_id),
    CONSTRAINT fk_return_shipments_request
        FOREIGN KEY (return_request_id)
            REFERENCES ordering.return_requests (return_request_id),
    CONSTRAINT chk_return_shipments_status
        CHECK (status IN (
            'CREATED','AWAITING_PICKUP','PICKED_UP','IN_TRANSIT','RECEIVED'
        ))
);

-- One active return shipment per return request
CREATE UNIQUE INDEX uq_return_shipments_request
    ON shipping.return_shipments (return_request_id)
    WHERE is_deleted = FALSE;

-- Return shipment lookup by return request
CREATE INDEX idx_ship_return_request
    ON shipping.return_shipments (return_request_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- Apply updated_at trigger to shipping schema tables
-- ===========================================================================
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'shipping'
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
