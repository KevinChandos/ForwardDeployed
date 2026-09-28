-- =============================================================================
-- V1__initial_schema.sql
-- Book Worm — Full PostgreSQL schema bootstrap
--
-- WHY: Creates every schema, table, unique constraint, check constraint,
--      foreign key, and index defined in Architecture/Database Design.md.
--      All tables carry the standard audit + soft-delete + optimistic-lock
--      columns required by the design.  Executed once on a blank database;
--      subsequent versions layer seed data on top.
--
-- Target: PostgreSQL 15+
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0. Extensions
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- provides gen_random_uuid()

-- ---------------------------------------------------------------------------
-- 1. Schemas (bounded-context namespaces)
-- ---------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS catalogue;
CREATE SCHEMA IF NOT EXISTS store;
CREATE SCHEMA IF NOT EXISTS discovery;
CREATE SCHEMA IF NOT EXISTS cart;
CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS promotions;
CREATE SCHEMA IF NOT EXISTS checkout;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS wallet;
CREATE SCHEMA IF NOT EXISTS shipping;
CREATE SCHEMA IF NOT EXISTS review;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS outbox;

-- =============================================================================
-- 2. IDENTITY schema
-- =============================================================================

-- identity.members
-- WHY: Central member aggregate; every other user-bound table FK-points here.
CREATE TABLE identity.members (
    member_id      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    display_name   VARCHAR(150)  NOT NULL,
    email          VARCHAR(320)  NULL,
    phone_number   VARCHAR(20)   NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     UUID          NULL,
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_members_status   CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')),
    CONSTRAINT chk_members_contact  CHECK (email IS NOT NULL OR phone_number IS NOT NULL)
);

CREATE UNIQUE INDEX uq_members_email
    ON identity.members (email)
    WHERE email IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_members_phone
    ON identity.members (phone_number)
    WHERE phone_number IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_identity_members_email
    ON identity.members (email)
    WHERE email IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_identity_members_phone
    ON identity.members (phone_number)
    WHERE phone_number IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_identity_members_status
    ON identity.members (status)
    WHERE deleted_at IS NULL;

-- identity.credentials
-- WHY: Stores hashed secrets separately from member PII; supports EMAIL/PHONE
--      channels independently, allowing future auth methods without schema change.
CREATE TABLE identity.credentials (
    credential_id    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id        UUID          NOT NULL,
    channel          VARCHAR(10)   NOT NULL,
    hashed_secret    VARCHAR(255)  NOT NULL,
    reset_token      VARCHAR(255)  NULL,
    reset_expires_at TIMESTAMPTZ   NULL,
    -- audit
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       UUID          NULL,
    updated_by       UUID          NULL,
    deleted_at       TIMESTAMPTZ   NULL,
    version          INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_credentials_channel CHECK (channel IN ('EMAIL','PHONE')),
    CONSTRAINT fk_credentials_member   FOREIGN KEY (member_id)
        REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_credentials_member_channel
    ON identity.credentials (member_id, channel)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_identity_credentials_member
    ON identity.credentials (member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_identity_credentials_reset_token
    ON identity.credentials (reset_token)
    WHERE reset_token IS NOT NULL AND deleted_at IS NULL;

-- identity.member_addresses
-- WHY: Multiple saved addresses per member; is_default flag kept here to avoid
--      an extra join when loading the default delivery address.
CREATE TABLE identity.member_addresses (
    address_id   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id    UUID          NOT NULL,
    label        VARCHAR(100)  NULL,
    first_name   VARCHAR(100)  NOT NULL,
    last_name    VARCHAR(100)  NOT NULL,
    line1        VARCHAR(250)  NOT NULL,
    line2        VARCHAR(250)  NULL,
    city         VARCHAR(100)  NOT NULL,
    pin_code     VARCHAR(20)   NOT NULL,
    state        VARCHAR(100)  NOT NULL,
    country      CHAR(2)       NOT NULL DEFAULT 'IN',
    is_default   BOOLEAN       NOT NULL DEFAULT FALSE,
    -- audit
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by   UUID          NULL,
    updated_by   UUID          NULL,
    deleted_at   TIMESTAMPTZ   NULL,
    version      INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_member_addresses_member FOREIGN KEY (member_id)
        REFERENCES identity.members (member_id)
);

CREATE INDEX idx_identity_addresses_member
    ON identity.member_addresses (member_id)
    WHERE deleted_at IS NULL;

-- identity.sessions
-- WHY: Tracks active JWT sessions; hashes stored (never raw tokens) to prevent
--      secret leakage via DB read access.
CREATE TABLE identity.sessions (
    session_id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id           UUID          NOT NULL,
    access_token_hash   VARCHAR(255)  NOT NULL,
    refresh_token_hash  VARCHAR(255)  NOT NULL,
    expires_at          TIMESTAMPTZ   NOT NULL,
    device_info         VARCHAR(500)  NULL,
    -- audit
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID          NULL,
    updated_by          UUID          NULL,
    deleted_at          TIMESTAMPTZ   NULL,   -- soft delete = logout
    version             INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_sessions_member FOREIGN KEY (member_id)
        REFERENCES identity.members (member_id)
);

CREATE INDEX idx_identity_sessions_member
    ON identity.sessions (member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_identity_sessions_expires
    ON identity.sessions (expires_at)
    WHERE deleted_at IS NULL;

-- identity.member_roles
-- WHY: RBAC; a member may hold multiple roles (e.g. STORE_ADMIN on one store,
--      REGISTERED_USER platform-wide). Soft-delete = role revocation.
CREATE TABLE identity.member_roles (
    member_role_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id      UUID          NOT NULL,
    role_name      VARCHAR(50)   NOT NULL,
    granted_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     UUID          NULL,
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_member_roles_name CHECK (
        role_name IN ('GUEST','REGISTERED_USER','STORE_ADMIN','CATALOGUE_MANAGER','PLATFORM_ADMIN')
    ),
    CONSTRAINT fk_member_roles_member FOREIGN KEY (member_id)
        REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_member_roles_member_role
    ON identity.member_roles (member_id, role_name)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 3. CATALOGUE schema
-- =============================================================================

-- catalogue.authors
-- WHY: Author is an independent aggregate that books reference; decoupled from
--      identity.members to allow authors without platform accounts.
CREATE TABLE catalogue.authors (
    author_id  UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(300)   NOT NULL,
    bio        TEXT           NULL,
    photo_url  VARCHAR(2000)  NULL,
    is_active  BOOLEAN        NOT NULL DEFAULT TRUE,
    -- audit
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by UUID           NULL,
    updated_by UUID           NULL,
    deleted_at TIMESTAMPTZ    NULL,
    version    INTEGER        NOT NULL DEFAULT 1
);

CREATE INDEX idx_catalogue_authors_fts
    ON catalogue.authors USING GIN (to_tsvector('english', name))
    WHERE deleted_at IS NULL;

-- catalogue.publishers
-- WHY: Publisher data changes infrequently; isolated here to avoid denormalising
--      publisher fields into every book row.
CREATE TABLE catalogue.publishers (
    publisher_id UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name         VARCHAR(300)   NOT NULL,
    website      VARCHAR(2000)  NULL,
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    -- audit
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by   UUID           NULL,
    updated_by   UUID           NULL,
    deleted_at   TIMESTAMPTZ    NULL,
    version      INTEGER        NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX uq_publishers_name
    ON catalogue.publishers (name)
    WHERE deleted_at IS NULL;

-- catalogue.categories
-- WHY: Self-referencing parent_category_id supports arbitrary-depth trees without
--      a separate junction table; query via recursive CTE.
CREATE TABLE catalogue.categories (
    category_id        UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name               VARCHAR(150)  NOT NULL,
    slug               VARCHAR(150)  NOT NULL,
    parent_category_id UUID          NULL,
    -- audit
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         UUID          NULL,
    updated_by         UUID          NULL,
    deleted_at         TIMESTAMPTZ   NULL,
    version            INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id)
        REFERENCES catalogue.categories (category_id)
);

CREATE UNIQUE INDEX uq_categories_slug
    ON catalogue.categories (slug)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_categories_name
    ON catalogue.categories (name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_categories_parent
    ON catalogue.categories (parent_category_id)
    WHERE deleted_at IS NULL;

-- catalogue.books
-- WHY: Core product entity. sales_count and average_rating are denormalised
--      counters updated by domain events (ReviewPublished, OrderDelivered) to
--      avoid expensive aggregate queries on hot read paths.
CREATE TABLE catalogue.books (
    book_id           UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title             VARCHAR(500)   NOT NULL,
    synopsis          TEXT           NULL,
    language          VARCHAR(50)    NOT NULL,
    cover_image_url   VARCHAR(2000)  NULL,
    published_date    DATE           NULL,
    sales_count       INTEGER        NOT NULL DEFAULT 0,
    average_rating    NUMERIC(3,2)   NULL,
    review_count      INTEGER        NOT NULL DEFAULT 0,
    publisher_id      UUID           NULL,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    -- audit
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by        UUID           NULL,
    updated_by        UUID           NULL,
    deleted_at        TIMESTAMPTZ    NULL,
    version           INTEGER        NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_books_rating CHECK (
        average_rating IS NULL OR (average_rating >= 1 AND average_rating <= 5)
    ),
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id)
        REFERENCES catalogue.publishers (publisher_id)
);

CREATE INDEX idx_catalogue_books_title_fts
    ON catalogue.books USING GIN (to_tsvector('english', title))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_books_publisher
    ON catalogue.books (publisher_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_books_active
    ON catalogue.books (is_active, published_date DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_books_sales
    ON catalogue.books (sales_count DESC)
    WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_catalogue_books_rating
    ON catalogue.books (average_rating DESC NULLS LAST)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- identity.author_follows  (declared here because it FK-references catalogue.authors)
-- WHY: Cross-schema relation; placed after catalogue.authors to satisfy FK ordering.
CREATE TABLE identity.author_follows (
    follow_id   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id   UUID          NOT NULL,
    author_id   UUID          NOT NULL,
    followed_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- audit
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  UUID          NULL,
    updated_by  UUID          NULL,
    deleted_at  TIMESTAMPTZ   NULL,   -- soft delete = unfollow
    version     INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_author_follows_member FOREIGN KEY (member_id)
        REFERENCES identity.members (member_id),
    CONSTRAINT fk_author_follows_author FOREIGN KEY (author_id)
        REFERENCES catalogue.authors (author_id)
);

CREATE UNIQUE INDEX uq_author_follows_member_author
    ON identity.author_follows (member_id, author_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_identity_follows_member
    ON identity.author_follows (member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_identity_follows_author
    ON identity.author_follows (author_id)
    WHERE deleted_at IS NULL;

-- catalogue.book_authors
-- WHY: M:N with a role attribute; co-authors/editors need separate rows under
--      the same book.
CREATE TABLE catalogue.book_authors (
    book_author_id UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id        UUID         NOT NULL,
    author_id      UUID         NOT NULL,
    role           VARCHAR(50)  NOT NULL DEFAULT 'AUTHOR',
    -- audit
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     UUID         NULL,
    updated_by     UUID         NULL,
    deleted_at     TIMESTAMPTZ  NULL,
    version        INTEGER      NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_book_authors_book   FOREIGN KEY (book_id)   REFERENCES catalogue.books   (book_id),
    CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES catalogue.authors (author_id)
);

CREATE UNIQUE INDEX uq_book_authors_book_author_role
    ON catalogue.book_authors (book_id, author_id, role)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_book_authors_book
    ON catalogue.book_authors (book_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_book_authors_author
    ON catalogue.book_authors (author_id)
    WHERE deleted_at IS NULL;

-- catalogue.book_categories
-- WHY: M:N tag table; a book may span multiple genre hierarchies.
CREATE TABLE catalogue.book_categories (
    book_category_id UUID  NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id          UUID  NOT NULL,
    category_id      UUID  NOT NULL,
    -- audit
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID  NULL,
    updated_by       UUID  NULL,
    deleted_at       TIMESTAMPTZ NULL,
    version          INTEGER     NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_book_categories_book     FOREIGN KEY (book_id)     REFERENCES catalogue.books      (book_id),
    CONSTRAINT fk_book_categories_category FOREIGN KEY (category_id) REFERENCES catalogue.categories (category_id)
);

CREATE UNIQUE INDEX uq_book_categories_book_cat
    ON catalogue.book_categories (book_id, category_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_book_categories_book
    ON catalogue.book_categories (book_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_book_categories_cat
    ON catalogue.book_categories (category_id)
    WHERE deleted_at IS NULL;

-- catalogue.book_formats
-- WHY: One book can exist in multiple formats (PAPERBACK, HARDCOVER, EBOOK), each
--      with its own ISBN and page count; prices are per-format-per-store.
CREATE TABLE catalogue.book_formats (
    book_format_id UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id        UUID         NOT NULL,
    format_type    VARCHAR(20)  NOT NULL,
    isbn           VARCHAR(20)  NULL,
    page_count     INTEGER      NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     UUID         NULL,
    updated_by     UUID         NULL,
    deleted_at     TIMESTAMPTZ  NULL,
    version        INTEGER      NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_book_formats_type CHECK (format_type IN ('PAPERBACK','HARDCOVER','EBOOK')),
    CONSTRAINT fk_book_formats_book  FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id)
);

CREATE UNIQUE INDEX uq_book_formats_book_type
    ON catalogue.book_formats (book_id, format_type)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_book_formats_isbn
    ON catalogue.book_formats (isbn)
    WHERE isbn IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_catalogue_book_formats_book
    ON catalogue.book_formats (book_id)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 4. STORE schema
-- =============================================================================

-- store.stores
-- WHY: Multi-store platform; all pricing, coupons, and tax rules are scoped per
--      store to support regional price variations.
CREATE TABLE store.stores (
    store_id        UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    slug            VARCHAR(200)  NOT NULL,
    region          VARCHAR(100)  NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    owner_member_id UUID          NULL,
    -- audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          NULL,
    updated_by      UUID          NULL,
    deleted_at      TIMESTAMPTZ   NULL,
    version         INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_stores_status  CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT fk_stores_owner    FOREIGN KEY (owner_member_id)
        REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_stores_slug
    ON store.stores (slug)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_store_stores_slug
    ON store.stores (slug)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_store_stores_status
    ON store.stores (status)
    WHERE deleted_at IS NULL;

-- catalogue.book_prices (declared here because it FK-references store.stores)
-- WHY: Price is effective-dated per format+store; effective_to = NULL means
--      the price is currently active — closing a price sets effective_to,
--      enabling full price history without deletes.
CREATE TABLE catalogue.book_prices (
    book_price_id  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_format_id UUID          NOT NULL,
    store_id       UUID          NOT NULL,
    amount         NUMERIC(14,2) NOT NULL,
    currency       CHAR(3)       NOT NULL DEFAULT 'INR',
    effective_from TIMESTAMPTZ   NOT NULL DEFAULT now(),
    effective_to   TIMESTAMPTZ   NULL,
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     UUID          NULL,
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_book_prices_amount     CHECK (amount > 0),
    CONSTRAINT chk_book_prices_dates      CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT fk_book_prices_format      FOREIGN KEY (book_format_id) REFERENCES catalogue.book_formats (book_format_id),
    CONSTRAINT fk_book_prices_store       FOREIGN KEY (store_id)       REFERENCES store.stores (store_id)
);

CREATE INDEX idx_catalogue_book_prices_format
    ON catalogue.book_prices (book_format_id, store_id, effective_from DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_catalogue_book_prices_active
    ON catalogue.book_prices (book_format_id, store_id)
    WHERE effective_to IS NULL AND deleted_at IS NULL;

-- store.store_policies
-- WHY: Return window and free-delivery threshold are operational settings that
--      change rarely; partial unique index enforces exactly one active policy per store.
CREATE TABLE store.store_policies (
    policy_id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id                UUID          NOT NULL,
    return_window_days      INTEGER       NOT NULL DEFAULT 7,
    free_delivery_threshold NUMERIC(14,2) NULL,
    currency                CHAR(3)       NOT NULL DEFAULT 'INR',
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    -- audit
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by              UUID          NULL,
    updated_by              UUID          NULL,
    deleted_at              TIMESTAMPTZ   NULL,
    version                 INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_store_policies_store FOREIGN KEY (store_id) REFERENCES store.stores (store_id)
);

CREATE UNIQUE INDEX uq_store_policies_active
    ON store.store_policies (store_id)
    WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_store_policies_store
    ON store.store_policies (store_id)
    WHERE deleted_at IS NULL;

-- store.tax_rules
-- WHY: Tax rates vary by category and change on budget dates; effective-dating
--      avoids touching existing order totals when rates change.
CREATE TABLE store.tax_rules (
    tax_rule_id    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id       UUID          NOT NULL,
    tax_category   VARCHAR(100)  NOT NULL,
    rate_percent   NUMERIC(5,2)  NOT NULL,
    effective_from DATE          NOT NULL,
    effective_to   DATE          NULL,
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     UUID          NULL,
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_tax_rules_rate  CHECK (rate_percent >= 0 AND rate_percent <= 100),
    CONSTRAINT fk_tax_rules_store  FOREIGN KEY (store_id) REFERENCES store.stores (store_id)
);

CREATE INDEX idx_store_tax_rules_store
    ON store.tax_rules (store_id, effective_from DESC)
    WHERE deleted_at IS NULL;

-- store.delivery_thresholds
-- WHY: Tiered shipping costs are driven by order amount; stored as rows rather
--      than a JSONB column so each tier can be indexed and queried individually.
CREATE TABLE store.delivery_thresholds (
    threshold_id    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id        UUID          NOT NULL,
    min_order_amount NUMERIC(14,2) NOT NULL,
    shipping_cost   NUMERIC(14,2) NOT NULL,
    currency        CHAR(3)       NOT NULL DEFAULT 'INR',
    -- audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          NULL,
    updated_by      UUID          NULL,
    deleted_at      TIMESTAMPTZ   NULL,
    version         INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_delivery_thresholds_amounts CHECK (min_order_amount >= 0 AND shipping_cost >= 0),
    CONSTRAINT fk_delivery_thresholds_store    FOREIGN KEY (store_id) REFERENCES store.stores (store_id)
);

CREATE INDEX idx_store_thresholds_store
    ON store.delivery_thresholds (store_id, min_order_amount)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 5. DISCOVERY schema
-- =============================================================================

-- discovery.recommendation_profiles
-- WHY: Profiles exist for both logged-in members (member_id) and anonymous
--      guests (guest_token); merging occurs on login, so the same profile
--      accumulates signal before and after authentication.
CREATE TABLE discovery.recommendation_profiles (
    profile_id  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id   UUID          NULL,
    guest_token VARCHAR(200)  NULL,
    -- audit
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  UUID          NULL,
    updated_by  UUID          NULL,
    deleted_at  TIMESTAMPTZ   NULL,
    version     INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_rp_identity CHECK (member_id IS NOT NULL OR guest_token IS NOT NULL),
    CONSTRAINT fk_rp_member    FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_rp_member
    ON discovery.recommendation_profiles (member_id)
    WHERE member_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_rp_guest
    ON discovery.recommendation_profiles (guest_token)
    WHERE guest_token IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_disc_rp_member
    ON discovery.recommendation_profiles (member_id)
    WHERE member_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_disc_rp_guest
    ON discovery.recommendation_profiles (guest_token)
    WHERE guest_token IS NOT NULL AND deleted_at IS NULL;

-- discovery.recommended_books
-- WHY: Pre-computed recommendations are stored here by the ML/recommendation
--      service so the web tier only runs a fast keyed lookup, not scoring.
CREATE TABLE discovery.recommended_books (
    entry_id    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    profile_id  UUID          NOT NULL,
    book_id     UUID          NOT NULL,
    score       NUMERIC(6,4)  NOT NULL,
    reason      VARCHAR(30)   NOT NULL,
    -- audit
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  UUID          NULL,
    updated_by  UUID          NULL,
    deleted_at  TIMESTAMPTZ   NULL,
    version     INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_rec_books_reason CHECK (reason IN ('ORDER_HISTORY','CATEGORY_AFFINITY','AUTHOR_FOLLOW')),
    CONSTRAINT fk_rec_books_profile FOREIGN KEY (profile_id) REFERENCES discovery.recommendation_profiles (profile_id),
    CONSTRAINT fk_rec_books_book    FOREIGN KEY (book_id)    REFERENCES catalogue.books (book_id)
);

CREATE UNIQUE INDEX uq_rec_books_profile_book
    ON discovery.recommended_books (profile_id, book_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_disc_rec_profile
    ON discovery.recommended_books (profile_id, score DESC)
    WHERE deleted_at IS NULL;

-- discovery.featured_lists
-- WHY: Curated lists (Bestseller, New Launch) are time-bounded; effective_to = NULL
--      means the list is currently live.
CREATE TABLE discovery.featured_lists (
    list_id        UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id       UUID         NOT NULL,
    list_type      VARCHAR(30)  NOT NULL,
    effective_from TIMESTAMPTZ  NOT NULL DEFAULT now(),
    effective_to   TIMESTAMPTZ  NULL,
    -- audit
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     UUID         NULL,
    updated_by     UUID         NULL,
    deleted_at     TIMESTAMPTZ  NULL,
    version        INTEGER      NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_featured_lists_type CHECK (list_type IN ('BESTSELLER','NEW_LAUNCH','RECOMMENDED')),
    CONSTRAINT fk_featured_lists_store FOREIGN KEY (store_id) REFERENCES store.stores (store_id)
);

CREATE INDEX idx_disc_fl_store_type
    ON discovery.featured_lists (store_id, list_type, effective_from DESC)
    WHERE deleted_at IS NULL;

-- discovery.featured_entries
-- WHY: rank allows explicit editorial ordering within a list; two unique indexes
--      prevent the same book appearing twice and prevent two books sharing a rank.
CREATE TABLE discovery.featured_entries (
    entry_id   UUID     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    list_id    UUID     NOT NULL,
    book_id    UUID     NOT NULL,
    rank       INTEGER  NOT NULL,
    -- audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID     NULL,
    updated_by UUID     NULL,
    deleted_at TIMESTAMPTZ NULL,
    version    INTEGER  NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_featured_entries_rank  CHECK (rank >= 1),
    CONSTRAINT fk_featured_entries_list   FOREIGN KEY (list_id)  REFERENCES discovery.featured_lists (list_id),
    CONSTRAINT fk_featured_entries_book   FOREIGN KEY (book_id)  REFERENCES catalogue.books (book_id)
);

CREATE UNIQUE INDEX uq_fe_list_book
    ON discovery.featured_entries (list_id, book_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_fe_list_rank
    ON discovery.featured_entries (list_id, rank)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_disc_fe_list_rank
    ON discovery.featured_entries (list_id, rank ASC)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 6. CART schema
-- =============================================================================

-- cart.carts
-- WHY: Guest carts (guest_token) are merged into the member cart on login;
--      status tracks lifecycle to prevent double-checkout.
CREATE TABLE cart.carts (
    cart_id     UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id   UUID          NULL,
    guest_token VARCHAR(200)  NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- audit
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  UUID          NULL,
    updated_by  UUID          NULL,
    deleted_at  TIMESTAMPTZ   NULL,
    version     INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_carts_identity CHECK (member_id IS NOT NULL OR guest_token IS NOT NULL),
    CONSTRAINT chk_carts_status   CHECK (status IN ('ACTIVE','CHECKING_OUT','CONVERTED','MERGED')),
    CONSTRAINT fk_carts_member    FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_carts_active_member
    ON cart.carts (member_id)
    WHERE status = 'ACTIVE' AND member_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_carts_active_guest
    ON cart.carts (guest_token)
    WHERE status = 'ACTIVE' AND guest_token IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_cart_carts_member
    ON cart.carts (member_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_cart_carts_guest
    ON cart.carts (guest_token)
    WHERE guest_token IS NOT NULL AND deleted_at IS NULL;

-- cart.cart_items
-- WHY: unit_price is snapshotted at add-time so price changes mid-session do not
--      silently alter what the member sees in their cart.
CREATE TABLE cart.cart_items (
    cart_item_id   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cart_id        UUID          NOT NULL,
    book_id        UUID          NOT NULL,
    book_format_id UUID          NOT NULL,
    quantity       INTEGER       NOT NULL DEFAULT 1,
    unit_price     NUMERIC(14,2) NOT NULL,
    currency       CHAR(3)       NOT NULL DEFAULT 'INR',
    added_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     UUID          NULL,
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_cart_items_qty        CHECK (quantity >= 1),
    CONSTRAINT chk_cart_items_price      CHECK (unit_price > 0),
    CONSTRAINT fk_cart_items_cart        FOREIGN KEY (cart_id)        REFERENCES cart.carts              (cart_id),
    CONSTRAINT fk_cart_items_book        FOREIGN KEY (book_id)        REFERENCES catalogue.books         (book_id),
    CONSTRAINT fk_cart_items_book_format FOREIGN KEY (book_format_id) REFERENCES catalogue.book_formats  (book_format_id)
);

CREATE UNIQUE INDEX uq_cart_items_cart_format
    ON cart.cart_items (cart_id, book_format_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_cart_items_cart
    ON cart.cart_items (cart_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_cart_items_book
    ON cart.cart_items (book_id)
    WHERE deleted_at IS NULL;

-- cart.wishlists
-- WHY: One wishlist per member; separated from cart so wishlist items survive
--      cart expiry/conversion without extra state management.
CREATE TABLE cart.wishlists (
    wishlist_id UUID  NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id   UUID  NOT NULL,
    -- audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID  NULL,
    updated_by  UUID  NULL,
    deleted_at  TIMESTAMPTZ NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_wishlists_member FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_wishlists_member
    ON cart.wishlists (member_id)
    WHERE deleted_at IS NULL;

-- cart.wishlist_items
-- WHY: Tracks format-level saves so moving from wishlist → cart pre-selects the
--      correct edition.
CREATE TABLE cart.wishlist_items (
    wishlist_item_id UUID  NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    wishlist_id      UUID  NOT NULL,
    book_id          UUID  NOT NULL,
    book_format_id   UUID  NOT NULL,
    added_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- audit
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID  NULL,
    updated_by       UUID  NULL,
    deleted_at       TIMESTAMPTZ NULL,
    version          INTEGER     NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id)    REFERENCES cart.wishlists          (wishlist_id),
    CONSTRAINT fk_wishlist_items_book     FOREIGN KEY (book_id)        REFERENCES catalogue.books         (book_id),
    CONSTRAINT fk_wishlist_items_format   FOREIGN KEY (book_format_id) REFERENCES catalogue.book_formats  (book_format_id)
);

CREATE UNIQUE INDEX uq_wishlist_items_list_format
    ON cart.wishlist_items (wishlist_id, book_format_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_cart_wishlist_items_wishlist
    ON cart.wishlist_items (wishlist_id)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 7. PROMOTIONS schema (before ORDERING — orders FK coupons)
-- =============================================================================

-- promotions.coupons
-- WHY: Coupon validity is store-scoped; used_count is incremented atomically
--      (SELECT … FOR UPDATE) during checkout to prevent over-redemption.
CREATE TABLE promotions.coupons (
    coupon_id       UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id        UUID          NOT NULL,
    code            VARCHAR(50)   NOT NULL,
    description     TEXT          NULL,
    discount_type   VARCHAR(10)   NOT NULL,
    discount_value  NUMERIC(14,2) NOT NULL,
    min_order_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    max_uses        INTEGER       NULL,
    used_count      INTEGER       NOT NULL DEFAULT 0,
    expires_at      TIMESTAMPTZ   NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    -- audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          NULL,
    updated_by      UUID          NULL,
    deleted_at      TIMESTAMPTZ   NULL,
    version         INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_coupons_type        CHECK (discount_type IN ('FLAT','PERCENT')),
    CONSTRAINT chk_coupons_value       CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_used        CHECK (used_count >= 0),
    CONSTRAINT chk_coupons_max_uses    CHECK (max_uses IS NULL OR used_count <= max_uses),
    CONSTRAINT fk_coupons_store        FOREIGN KEY (store_id) REFERENCES store.stores (store_id)
);

CREATE UNIQUE INDEX uq_coupons_store_code
    ON promotions.coupons (store_id, code)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_prom_coupons_code
    ON promotions.coupons (store_id, code)
    WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_prom_coupons_expires
    ON promotions.coupons (expires_at)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- promotions.gift_point_policies
-- WHY: Loyalty point earn/burn rates are store-specific; partial unique index
--      enforces a single active policy per store.
CREATE TABLE promotions.gift_point_policies (
    policy_id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    store_id               UUID          NOT NULL,
    points_per_rupee       NUMERIC(8,4)  NOT NULL,
    rupees_per_point       NUMERIC(8,4)  NOT NULL,
    max_redemption_percent NUMERIC(5,2)  NOT NULL,
    is_active              BOOLEAN       NOT NULL DEFAULT TRUE,
    -- audit
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             UUID          NULL,
    updated_by             UUID          NULL,
    deleted_at             TIMESTAMPTZ   NULL,
    version                INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_gpp_values CHECK (
        points_per_rupee > 0 AND rupees_per_point > 0
        AND max_redemption_percent > 0 AND max_redemption_percent <= 100
    ),
    CONSTRAINT fk_gpp_store FOREIGN KEY (store_id) REFERENCES store.stores (store_id)
);

CREATE UNIQUE INDEX uq_gpp_active_store
    ON promotions.gift_point_policies (store_id)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- =============================================================================
-- 8. ORDERING schema
-- =============================================================================

-- ordering.orders
-- WHY: Includes snapshot fields (coupon_code_snapshot, wallet_debit_amount) so
--      historical order display is immune to promotions data changes.
CREATE TABLE ordering.orders (
    order_id             UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id            UUID          NULL,
    guest_email          VARCHAR(320)  NULL,
    store_id             UUID          NOT NULL,
    status               VARCHAR(30)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    subtotal             NUMERIC(14,2) NOT NULL,
    tax_amount           NUMERIC(14,2) NOT NULL DEFAULT 0,
    shipping_amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    grand_total          NUMERIC(14,2) NOT NULL,
    currency             CHAR(3)       NOT NULL DEFAULT 'INR',
    coupon_id            UUID          NULL,
    coupon_code_snapshot VARCHAR(50)   NULL,
    wallet_debit_amount  NUMERIC(14,2) NOT NULL DEFAULT 0,
    placed_at            TIMESTAMPTZ   NULL,
    confirmed_at         TIMESTAMPTZ   NULL,
    cancelled_at         TIMESTAMPTZ   NULL,
    delivered_at         TIMESTAMPTZ   NULL,
    cancellation_reason  TEXT          NULL,
    -- audit
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           UUID          NULL,
    updated_by           UUID          NULL,
    deleted_at           TIMESTAMPTZ   NULL,
    version              INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_orders_contact CHECK (member_id IS NOT NULL OR guest_email IS NOT NULL),
    CONSTRAINT chk_orders_total   CHECK (grand_total >= 0),
    CONSTRAINT chk_orders_status  CHECK (status IN (
        'PENDING_PAYMENT','AWAITING_PAYMENT','CONFIRMED','PROCESSING',
        'DISPATCHED','DELIVERED','CANCELLED','RETURN_REQUESTED',
        'RETURN_APPROVED','RETURN_REJECTED','RETURN_IN_TRANSIT',
        'RETURN_RECEIVED','REFUNDED'
    )),
    CONSTRAINT fk_orders_member  FOREIGN KEY (member_id)  REFERENCES identity.members   (member_id),
    CONSTRAINT fk_orders_store   FOREIGN KEY (store_id)   REFERENCES store.stores        (store_id),
    CONSTRAINT fk_orders_coupon  FOREIGN KEY (coupon_id)  REFERENCES promotions.coupons  (coupon_id)
);

CREATE INDEX idx_ord_orders_member
    ON ordering.orders (member_id, placed_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ord_orders_status
    ON ordering.orders (status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ord_orders_store
    ON ordering.orders (store_id, placed_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ord_orders_placed_at
    ON ordering.orders (placed_at DESC)
    WHERE deleted_at IS NULL;

-- promotions.coupon_redemptions (declared here — FK on ordering.orders)
-- WHY: Tracks which order redeemed which coupon; soft-delete = coupon released
--      when the order is cancelled, allowing the member to reuse it.
CREATE TABLE promotions.coupon_redemptions (
    redemption_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    coupon_id     UUID          NOT NULL,
    member_id     UUID          NULL,
    guest_token   VARCHAR(200)  NULL,
    order_id      UUID          NOT NULL,
    redeemed_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- audit
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by    UUID          NULL,
    updated_by    UUID          NULL,
    deleted_at    TIMESTAMPTZ   NULL,
    version       INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_cr_coupon FOREIGN KEY (coupon_id)  REFERENCES promotions.coupons   (coupon_id),
    CONSTRAINT fk_cr_member FOREIGN KEY (member_id)  REFERENCES identity.members      (member_id),
    CONSTRAINT fk_cr_order  FOREIGN KEY (order_id)   REFERENCES ordering.orders       (order_id)
);

CREATE UNIQUE INDEX uq_coupon_redemptions_order
    ON promotions.coupon_redemptions (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_prom_redemptions_member
    ON promotions.coupon_redemptions (member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_prom_redemptions_coupon
    ON promotions.coupon_redemptions (coupon_id)
    WHERE deleted_at IS NULL;

-- ordering.order_delivery_addresses
-- WHY: Snapshot of address at order-time; never modifies the member's saved
--      address after the order is placed.
CREATE TABLE ordering.order_delivery_addresses (
    delivery_address_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id            UUID          NOT NULL,
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NOT NULL,
    line1               VARCHAR(250)  NOT NULL,
    line2               VARCHAR(250)  NULL,
    city                VARCHAR(100)  NOT NULL,
    pin_code            VARCHAR(20)   NOT NULL,
    state               VARCHAR(100)  NOT NULL,
    country             CHAR(2)       NOT NULL DEFAULT 'IN',
    email               VARCHAR(320)  NOT NULL,
    phone               VARCHAR(20)   NOT NULL,
    -- audit
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID          NULL,
    updated_by          UUID          NULL,
    deleted_at          TIMESTAMPTZ   NULL,
    version             INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_oda_order FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id)
);

CREATE UNIQUE INDEX uq_oda_order
    ON ordering.order_delivery_addresses (order_id)
    WHERE deleted_at IS NULL;

-- ordering.order_lines
-- WHY: title_snapshot / format_snapshot decouple order history from catalogue
--      changes; a book renamed after purchase still shows the original title.
CREATE TABLE ordering.order_lines (
    order_line_id   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id        UUID          NOT NULL,
    book_id         UUID          NOT NULL,
    book_format_id  UUID          NOT NULL,
    title_snapshot  VARCHAR(500)  NOT NULL,
    author_snapshot VARCHAR(500)  NULL,
    format_snapshot VARCHAR(20)   NOT NULL,
    quantity        INTEGER       NOT NULL,
    unit_price      NUMERIC(14,2) NOT NULL,
    subtotal        NUMERIC(14,2) NOT NULL,
    currency        CHAR(3)       NOT NULL DEFAULT 'INR',
    -- audit
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          NULL,
    updated_by      UUID          NULL,
    deleted_at      TIMESTAMPTZ   NULL,
    version         INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_order_lines_qty      CHECK (quantity >= 1),
    CONSTRAINT chk_order_lines_price    CHECK (unit_price > 0),
    CONSTRAINT chk_order_lines_subtotal CHECK (subtotal = unit_price * quantity),
    CONSTRAINT fk_order_lines_order  FOREIGN KEY (order_id)       REFERENCES ordering.orders          (order_id),
    CONSTRAINT fk_order_lines_book   FOREIGN KEY (book_id)        REFERENCES catalogue.books           (book_id),
    CONSTRAINT fk_order_lines_format FOREIGN KEY (book_format_id) REFERENCES catalogue.book_formats    (book_format_id)
);

CREATE INDEX idx_ord_lines_order
    ON ordering.order_lines (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ord_lines_book
    ON ordering.order_lines (book_id)
    WHERE deleted_at IS NULL;

-- ordering.return_requests
CREATE TABLE ordering.return_requests (
    return_request_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id          UUID          NOT NULL,
    status            VARCHAR(30)   NOT NULL DEFAULT 'REQUESTED',
    reason            VARCHAR(200)  NOT NULL,
    notes             TEXT          NULL,
    requested_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMPTZ   NULL,
    -- audit
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        UUID          NULL,
    updated_by        UUID          NULL,
    deleted_at        TIMESTAMPTZ   NULL,
    version           INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_return_requests_status CHECK (
        status IN ('REQUESTED','APPROVED','REJECTED','IN_TRANSIT','RECEIVED','REFUNDED')
    ),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id)
);

CREATE INDEX idx_ord_return_requests_order
    ON ordering.return_requests (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ord_return_requests_status
    ON ordering.return_requests (status)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 9. CHECKOUT schema
-- =============================================================================

-- checkout.checkout_sessions
-- WHY: Short-lived session (TTL via expires_at) locks pricing and coupon state
--      during payment flow; prevents stale-price attacks between add-to-cart
--      and payment confirmation.
CREATE TABLE checkout.checkout_sessions (
    session_id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cart_id             UUID          NOT NULL,
    member_id           UUID          NULL,
    guest_token         VARCHAR(200)  NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    expires_at          TIMESTAMPTZ   NOT NULL,
    coupon_id           UUID          NULL,
    wallet_debit_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    subtotal            NUMERIC(14,2) NULL,
    tax_amount          NUMERIC(14,2) NULL,
    shipping_amount     NUMERIC(14,2) NULL,
    discount_amount     NUMERIC(14,2) NULL,
    grand_total         NUMERIC(14,2) NULL,
    currency            CHAR(3)       NOT NULL DEFAULT 'INR',
    -- audit
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID          NULL,
    updated_by          UUID          NULL,
    deleted_at          TIMESTAMPTZ   NULL,
    version             INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_cs_identity CHECK (member_id IS NOT NULL OR guest_token IS NOT NULL),
    CONSTRAINT chk_cs_status   CHECK (
        status IN ('CREATED','ADDRESS_SET','PRICING_APPLIED','CONFIRMED','COMPLETED','EXPIRED')
    ),
    CONSTRAINT fk_cs_cart    FOREIGN KEY (cart_id)    REFERENCES cart.carts          (cart_id),
    CONSTRAINT fk_cs_member  FOREIGN KEY (member_id)  REFERENCES identity.members    (member_id),
    CONSTRAINT fk_cs_coupon  FOREIGN KEY (coupon_id)  REFERENCES promotions.coupons  (coupon_id)
);

CREATE INDEX idx_chk_sessions_member
    ON checkout.checkout_sessions (member_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_chk_sessions_cart
    ON checkout.checkout_sessions (cart_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_chk_sessions_expires
    ON checkout.checkout_sessions (expires_at)
    WHERE status NOT IN ('COMPLETED','EXPIRED') AND deleted_at IS NULL;

-- checkout.checkout_addresses
CREATE TABLE checkout.checkout_addresses (
    checkout_address_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id          UUID          NOT NULL,
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NOT NULL,
    line1               VARCHAR(250)  NOT NULL,
    line2               VARCHAR(250)  NULL,
    city                VARCHAR(100)  NOT NULL,
    pin_code            VARCHAR(20)   NOT NULL,
    state               VARCHAR(100)  NOT NULL,
    country             CHAR(2)       NOT NULL DEFAULT 'IN',
    email               VARCHAR(320)  NOT NULL,
    phone               VARCHAR(20)   NOT NULL,
    -- audit
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID          NULL,
    updated_by          UUID          NULL,
    deleted_at          TIMESTAMPTZ   NULL,
    version             INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_checkout_address_session FOREIGN KEY (session_id)
        REFERENCES checkout.checkout_sessions (session_id)
);

CREATE UNIQUE INDEX uq_checkout_address_session
    ON checkout.checkout_addresses (session_id)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 10. PAYMENT schema
-- =============================================================================

-- payment.payment_transactions
-- WHY: gateway_id is the external reference; masked_card_number stores only
--      the last-4 pattern — raw card data is never persisted (PCI-DSS).
CREATE TABLE payment.payment_transactions (
    transaction_id      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id            UUID          NOT NULL,
    gateway_id          VARCHAR(200)  NULL,
    gateway_name        VARCHAR(100)  NULL,
    payment_method      VARCHAR(20)   NOT NULL,
    amount              NUMERIC(14,2) NOT NULL,
    currency            CHAR(3)       NOT NULL DEFAULT 'INR',
    status              VARCHAR(20)   NOT NULL DEFAULT 'INITIATED',
    masked_card_number  VARCHAR(20)   NULL,
    cardholder_name     VARCHAR(200)  NULL,
    card_expiry_month   SMALLINT      NULL,
    card_expiry_year    SMALLINT      NULL,
    upi_id              VARCHAR(100)  NULL,
    confirmed_at        TIMESTAMPTZ   NULL,
    failed_at           TIMESTAMPTZ   NULL,
    -- audit
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID          NULL,
    updated_by          UUID          NULL,
    deleted_at          TIMESTAMPTZ   NULL,
    version             INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_pay_txn_method   CHECK (payment_method IN ('CREDIT_CARD','DEBIT_CARD','UPI','WALLET')),
    CONSTRAINT chk_pay_txn_amount   CHECK (amount > 0),
    CONSTRAINT chk_pay_txn_status   CHECK (status IN (
        'INITIATED','PENDING','CONFIRMED','FAILED','TIMED_OUT',
        'REFUND_PENDING','REFUNDED','REFUND_FAILED'
    )),
    CONSTRAINT chk_pay_txn_expiry   CHECK (card_expiry_month IS NULL OR card_expiry_month BETWEEN 1 AND 12),
    CONSTRAINT fk_pay_txn_order     FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id)
);

CREATE INDEX idx_pay_txn_order
    ON payment.payment_transactions (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_pay_txn_status
    ON payment.payment_transactions (status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_pay_txn_gateway
    ON payment.payment_transactions (gateway_id)
    WHERE gateway_id IS NOT NULL AND deleted_at IS NULL;

-- payment.payment_attempts
-- WHY: Each retry on a transaction produces a new attempt row, giving a full
--      audit trail of gateway interactions without mutating the parent transaction.
CREATE TABLE payment.payment_attempts (
    attempt_id     UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    transaction_id UUID          NOT NULL,
    attempted_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    gateway_status VARCHAR(100)  NOT NULL,
    failure_reason TEXT          NULL,
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     UUID          NULL,
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,
    version        INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_pay_attempts_txn FOREIGN KEY (transaction_id)
        REFERENCES payment.payment_transactions (transaction_id)
);

CREATE INDEX idx_pay_attempts_txn
    ON payment.payment_attempts (transaction_id, attempted_at DESC)
    WHERE deleted_at IS NULL;

-- payment.refunds
CREATE TABLE payment.refunds (
    refund_id        UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    transaction_id   UUID          NOT NULL,
    order_id         UUID          NOT NULL,
    amount           NUMERIC(14,2) NOT NULL,
    currency         CHAR(3)       NOT NULL DEFAULT 'INR',
    reason           VARCHAR(200)  NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'INITIATED',
    gateway_refund_id VARCHAR(200) NULL,
    initiated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ   NULL,
    -- audit
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       UUID          NULL,
    updated_by       UUID          NULL,
    deleted_at       TIMESTAMPTZ   NULL,
    version          INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_refunds_amount CHECK (amount > 0),
    CONSTRAINT chk_refunds_status CHECK (status IN ('INITIATED','PROCESSING','COMPLETED','FAILED')),
    CONSTRAINT fk_refunds_txn   FOREIGN KEY (transaction_id) REFERENCES payment.payment_transactions (transaction_id),
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id)       REFERENCES ordering.orders              (order_id)
);

CREATE INDEX idx_pay_refunds_txn
    ON payment.refunds (transaction_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_pay_refunds_order
    ON payment.refunds (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_pay_refunds_status
    ON payment.refunds (status)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 11. WALLET schema
-- =============================================================================

-- wallet.wallet_accounts
-- WHY: One wallet per member; balance must never go negative — enforced by CHECK
--      and application-level optimistic-lock (version) during debit.
CREATE TABLE wallet.wallet_accounts (
    wallet_id  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    member_id  UUID          NOT NULL,
    balance    NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    currency   CHAR(3)       NOT NULL DEFAULT 'INR',
    status     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- audit
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by UUID          NULL,
    updated_by UUID          NULL,
    deleted_at TIMESTAMPTZ   NULL,
    version    INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_wallet_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallet_status  CHECK (status IN ('ACTIVE','FROZEN')),
    CONSTRAINT fk_wallet_member   FOREIGN KEY (member_id) REFERENCES identity.members (member_id)
);

CREATE UNIQUE INDEX uq_wallet_member
    ON wallet.wallet_accounts (member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_wal_accounts_member
    ON wallet.wallet_accounts (member_id)
    WHERE deleted_at IS NULL;

-- wallet.wallet_transactions
-- WHY: Immutable ledger rows; balance_after provides a running snapshot so
--      recalculation is not needed when displaying mini-statements.
CREATE TABLE wallet.wallet_transactions (
    wallet_txn_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    wallet_id     UUID          NOT NULL,
    txn_type      VARCHAR(10)   NOT NULL,
    amount        NUMERIC(14,2) NOT NULL,
    source        VARCHAR(20)   NOT NULL,
    reference_id  UUID          NULL,
    balance_after NUMERIC(14,2) NOT NULL,
    -- audit (immutable after insert)
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by    UUID          NULL,
    updated_by    UUID          NULL,
    deleted_at    TIMESTAMPTZ   NULL,
    version       INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_wal_txn_type   CHECK (txn_type IN ('CREDIT','DEBIT')),
    CONSTRAINT chk_wal_txn_source CHECK (source IN ('REFUND','GIFT','REDEMPTION','ADJUSTMENT')),
    CONSTRAINT chk_wal_txn_amount CHECK (amount > 0),
    CONSTRAINT fk_wal_txn_wallet  FOREIGN KEY (wallet_id) REFERENCES wallet.wallet_accounts (wallet_id)
);

CREATE INDEX idx_wal_transactions_wallet
    ON wallet.wallet_transactions (wallet_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_wal_transactions_reference
    ON wallet.wallet_transactions (reference_id)
    WHERE reference_id IS NOT NULL AND deleted_at IS NULL;

-- =============================================================================
-- 12. SHIPPING schema
-- =============================================================================

CREATE TABLE shipping.shipments (
    shipment_id             UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id                UUID          NOT NULL,
    carrier                 VARCHAR(100)  NULL,
    tracking_number         VARCHAR(200)  NULL,
    tracking_url            VARCHAR(2000) NULL,
    estimated_delivery_date DATE          NULL,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'CREATED',
    dispatched_at           TIMESTAMPTZ   NULL,
    delivered_at            TIMESTAMPTZ   NULL,
    -- audit
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by              UUID          NULL,
    updated_by              UUID          NULL,
    deleted_at              TIMESTAMPTZ   NULL,
    version                 INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_shipments_status CHECK (
        status IN ('CREATED','READY_FOR_DISPATCH','DISPATCHED','IN_TRANSIT',
                   'OUT_FOR_DELIVERY','DELIVERED','CANCELLED')
    ),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES ordering.orders (order_id)
);

CREATE UNIQUE INDEX uq_shipments_order
    ON shipping.shipments (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ship_shipments_order
    ON shipping.shipments (order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ship_shipments_status
    ON shipping.shipments (status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ship_shipments_tracking
    ON shipping.shipments (tracking_number)
    WHERE tracking_number IS NOT NULL AND deleted_at IS NULL;

-- shipping.shipment_events
-- WHY: Immutable event log per shipment; append-only for carrier webhook updates.
CREATE TABLE shipping.shipment_events (
    event_id    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    shipment_id UUID          NOT NULL,
    event_type  VARCHAR(50)   NOT NULL,
    location    VARCHAR(300)  NULL,
    notes       TEXT          NULL,
    occurred_at TIMESTAMPTZ   NOT NULL,
    -- audit
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  UUID          NULL,
    updated_by  UUID          NULL,
    deleted_at  TIMESTAMPTZ   NULL,
    version     INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT fk_shipment_events_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipping.shipments (shipment_id)
);

CREATE INDEX idx_ship_events_shipment
    ON shipping.shipment_events (shipment_id, occurred_at ASC)
    WHERE deleted_at IS NULL;

-- shipping.return_shipments
CREATE TABLE shipping.return_shipments (
    return_shipment_id UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    return_request_id  UUID          NOT NULL,
    carrier            VARCHAR(100)  NULL,
    tracking_number    VARCHAR(200)  NULL,
    tracking_url       VARCHAR(2000) NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'CREATED',
    picked_up_at       TIMESTAMPTZ   NULL,
    received_at        TIMESTAMPTZ   NULL,
    -- audit
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         UUID          NULL,
    updated_by         UUID          NULL,
    deleted_at         TIMESTAMPTZ   NULL,
    version            INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_return_shipments_status CHECK (
        status IN ('CREATED','AWAITING_PICKUP','PICKED_UP','IN_TRANSIT','RECEIVED')
    ),
    CONSTRAINT fk_return_shipments_request FOREIGN KEY (return_request_id)
        REFERENCES ordering.return_requests (return_request_id)
);

CREATE UNIQUE INDEX uq_return_shipments_request
    ON shipping.return_shipments (return_request_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ship_return_request
    ON shipping.return_shipments (return_request_id)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 13. REVIEW schema
-- =============================================================================

-- review.reviews
-- WHY: Unique constraint prevents duplicate reviews; soft delete + status column
--      supports moderation without permanent data loss.
CREATE TABLE review.reviews (
    review_id        UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id          UUID          NOT NULL,
    member_id        UUID          NOT NULL,
    rating           SMALLINT      NOT NULL,
    body             TEXT          NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    submitted_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at     TIMESTAMPTZ   NULL,
    rejection_reason VARCHAR(500)  NULL,
    -- audit
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       UUID          NULL,
    updated_by       UUID          NULL,
    deleted_at       TIMESTAMPTZ   NULL,
    version          INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_status CHECK (status IN ('PENDING','PUBLISHED','REJECTED')),
    CONSTRAINT fk_reviews_book    FOREIGN KEY (book_id)   REFERENCES catalogue.books    (book_id),
    CONSTRAINT fk_reviews_member  FOREIGN KEY (member_id) REFERENCES identity.members   (member_id)
);

CREATE UNIQUE INDEX uq_reviews_book_member
    ON review.reviews (book_id, member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_rev_reviews_book
    ON review.reviews (book_id, status, published_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_rev_reviews_member
    ON review.reviews (member_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_rev_reviews_status
    ON review.reviews (status)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 14. NOTIFICATION schema
-- =============================================================================

-- notification.notification_templates
-- WHY: Natural PK (template_code) avoids an extra join on the event table and
--      is safe because codes are immutable business identifiers.
CREATE TABLE notification.notification_templates (
    template_code VARCHAR(100) NOT NULL PRIMARY KEY,
    subject       VARCHAR(500) NULL,
    body_template TEXT         NOT NULL,
    channel       VARCHAR(10)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    UUID         NULL,
    updated_by    UUID         NULL,
    deleted_at    TIMESTAMPTZ  NULL,
    version       INTEGER      NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_notif_tmpl_channel CHECK (channel IN ('EMAIL','SMS','PUSH'))
);

-- notification.notification_events
-- WHY: payload is JSONB — templates are rendered server-side at send time using
--      the stored payload, so the pre-rendered content is never persisted.
CREATE TABLE notification.notification_events (
    notification_id    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    recipient_member_id UUID         NULL,
    recipient_email    VARCHAR(320)  NULL,
    channel            VARCHAR(10)   NOT NULL,
    template_code      VARCHAR(100)  NOT NULL,
    payload            JSONB         NOT NULL DEFAULT '{}',
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    sent_at            TIMESTAMPTZ   NULL,
    failure_reason     TEXT          NULL,
    -- audit
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         UUID          NULL,
    updated_by         UUID          NULL,
    deleted_at         TIMESTAMPTZ   NULL,
    version            INTEGER       NOT NULL DEFAULT 1,
    -- constraints
    CONSTRAINT chk_notif_ev_channel CHECK (channel IN ('EMAIL','SMS','PUSH')),
    CONSTRAINT chk_notif_ev_status  CHECK (status IN ('PENDING','SENT','FAILED','BOUNCED')),
    CONSTRAINT fk_notif_ev_member   FOREIGN KEY (recipient_member_id)
        REFERENCES identity.members (member_id),
    CONSTRAINT fk_notif_ev_template FOREIGN KEY (template_code)
        REFERENCES notification.notification_templates (template_code)
);

CREATE INDEX idx_notif_events_member
    ON notification.notification_events (recipient_member_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notif_events_status
    ON notification.notification_events (status)
    WHERE deleted_at IS NULL;

-- =============================================================================
-- 15. OUTBOX schema
-- =============================================================================

-- outbox.domain_event_outbox
-- WHY: Transactional outbox pattern — events are written in the same DB
--      transaction as the aggregate mutation, then relayed to the message bus
--      by a dedicated relay process.  This guarantees at-least-once delivery
--      without distributed transactions.
CREATE TABLE outbox.domain_event_outbox (
    event_id       UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    event_type     VARCHAR(200)  NOT NULL,
    aggregate_type VARCHAR(100)  NOT NULL,
    aggregate_id   UUID          NOT NULL,
    payload        JSONB         NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    occurred_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ   NULL,
    retry_count    INTEGER       NOT NULL DEFAULT 0,
    -- audit
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- constraints
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED'))
);

CREATE INDEX idx_outbox_status
    ON outbox.domain_event_outbox (status, occurred_at ASC)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_aggregate
    ON outbox.domain_event_outbox (aggregate_type, aggregate_id, occurred_at ASC);
