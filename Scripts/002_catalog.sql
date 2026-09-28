-- =============================================================================
-- 002_catalog.sql
-- Schemas: catalogue, discovery (book-related tables)
-- Covers: authors, publishers, categories, books, book_authors,
--         book_categories, book_formats, book_prices,
--         author_follows (identity → catalogue cross-schema),
--         discovery.recommended_books, discovery.featured_entries,
--         cart.cart_items, cart.wishlist_items
--
-- Prerequisites: 001_users.sql must be executed first.
--
-- Audit columns on every table:
--   created_at, created_by, updated_at, updated_by, version, is_deleted
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS catalogue;

-- ===========================================================================
-- SCHEMA: catalogue
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- catalogue.authors
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.authors (
    author_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(300)    NOT NULL,
    bio         TEXT            NULL,
    photo_url   VARCHAR(2000)   NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_authors PRIMARY KEY (author_id)
);

CREATE INDEX idx_catalogue_authors_fts
    ON catalogue.authors USING GIN (to_tsvector('english', name))
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.publishers
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.publishers (
    publisher_id    UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(300)    NOT NULL,
    website         VARCHAR(2000)   NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_publishers PRIMARY KEY (publisher_id)
);

CREATE UNIQUE INDEX uq_publishers_name
    ON catalogue.publishers (name)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.categories
-- (Self-referencing for hierarchical categories)
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.categories (
    category_id         UUID            NOT NULL DEFAULT gen_random_uuid(),
    name                VARCHAR(150)    NOT NULL,
    slug                VARCHAR(150)    NOT NULL,
    parent_category_id  UUID            NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_categories PRIMARY KEY (category_id),
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_category_id)
            REFERENCES catalogue.categories (category_id)
);

CREATE UNIQUE INDEX uq_categories_slug
    ON catalogue.categories (slug)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_categories_name
    ON catalogue.categories (name)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_catalogue_categories_parent
    ON catalogue.categories (parent_category_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.books
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.books (
    book_id             UUID            NOT NULL DEFAULT gen_random_uuid(),
    title               VARCHAR(500)    NOT NULL,
    synopsis            TEXT            NULL,
    language            VARCHAR(50)     NOT NULL,
    cover_image_url     VARCHAR(2000)   NULL,
    published_date      DATE            NULL,
    -- Denormalised counters updated by event handlers
    sales_count         INTEGER         NOT NULL DEFAULT 0,
    average_rating      NUMERIC(3,2)    NULL,
    review_count        INTEGER         NOT NULL DEFAULT 0,
    publisher_id        UUID            NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_books PRIMARY KEY (book_id),
    CONSTRAINT fk_books_publisher
        FOREIGN KEY (publisher_id)
            REFERENCES catalogue.publishers (publisher_id),
    CONSTRAINT chk_books_rating
        CHECK (average_rating IS NULL
               OR (average_rating >= 1 AND average_rating <= 5)),
    CONSTRAINT chk_books_sales_count
        CHECK (sales_count >= 0),
    CONSTRAINT chk_books_review_count
        CHECK (review_count >= 0)
);

-- Full-text search on title
CREATE INDEX idx_catalogue_books_title_fts
    ON catalogue.books USING GIN (to_tsvector('english', title))
    WHERE is_deleted = FALSE;

CREATE INDEX idx_catalogue_books_publisher
    ON catalogue.books (publisher_id)
    WHERE is_deleted = FALSE;

-- New launches: recently published active books
CREATE INDEX idx_catalogue_books_active
    ON catalogue.books (is_active, published_date DESC)
    WHERE is_deleted = FALSE;

-- Bestsellers sort
CREATE INDEX idx_catalogue_books_sales
    ON catalogue.books (sales_count DESC)
    WHERE is_active = TRUE AND is_deleted = FALSE;

-- Top-rated sort
CREATE INDEX idx_catalogue_books_rating
    ON catalogue.books (average_rating DESC NULLS LAST)
    WHERE is_active = TRUE AND is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.book_authors  (many-to-many junction)
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.book_authors (
    book_author_id  UUID            NOT NULL DEFAULT gen_random_uuid(),
    book_id         UUID            NOT NULL,
    author_id       UUID            NOT NULL,
    role            VARCHAR(50)     NOT NULL DEFAULT 'AUTHOR',

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_book_authors PRIMARY KEY (book_author_id),
    CONSTRAINT fk_book_authors_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT fk_book_authors_author
        FOREIGN KEY (author_id) REFERENCES catalogue.authors (author_id),
    CONSTRAINT chk_book_author_role
        CHECK (role IN ('AUTHOR','CO_AUTHOR','EDITOR','TRANSLATOR'))
);

CREATE UNIQUE INDEX uq_book_authors_book_author_role
    ON catalogue.book_authors (book_id, author_id, role)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_catalogue_book_authors_book
    ON catalogue.book_authors (book_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_catalogue_book_authors_author
    ON catalogue.book_authors (author_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.book_categories  (many-to-many junction)
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.book_categories (
    book_category_id    UUID    NOT NULL DEFAULT gen_random_uuid(),
    book_id             UUID    NOT NULL,
    category_id         UUID    NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_book_categories PRIMARY KEY (book_category_id),
    CONSTRAINT fk_book_categories_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT fk_book_categories_category
        FOREIGN KEY (category_id) REFERENCES catalogue.categories (category_id)
);

CREATE UNIQUE INDEX uq_book_categories_book_cat
    ON catalogue.book_categories (book_id, category_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_catalogue_book_categories_book
    ON catalogue.book_categories (book_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_catalogue_book_categories_cat
    ON catalogue.book_categories (category_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.book_formats
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.book_formats (
    book_format_id  UUID        NOT NULL DEFAULT gen_random_uuid(),
    book_id         UUID        NOT NULL,
    format_type     VARCHAR(20) NOT NULL,
    isbn            VARCHAR(20) NULL,
    page_count      INTEGER     NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_book_formats PRIMARY KEY (book_format_id),
    CONSTRAINT fk_book_formats_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT chk_book_formats_type
        CHECK (format_type IN ('PAPERBACK','HARDCOVER','EBOOK'))
);

CREATE UNIQUE INDEX uq_book_formats_book_type
    ON catalogue.book_formats (book_id, format_type)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_book_formats_isbn
    ON catalogue.book_formats (isbn)
    WHERE isbn IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX idx_catalogue_book_formats_book
    ON catalogue.book_formats (book_id)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- catalogue.book_prices
-- ---------------------------------------------------------------------------
CREATE TABLE catalogue.book_prices (
    book_price_id   UUID            NOT NULL DEFAULT gen_random_uuid(),
    book_format_id  UUID            NOT NULL,
    store_id        UUID            NOT NULL,
    amount          NUMERIC(14,2)   NOT NULL,
    currency        CHAR(3)         NOT NULL DEFAULT 'INR',
    effective_from  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    effective_to    TIMESTAMPTZ     NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_book_prices PRIMARY KEY (book_price_id),
    CONSTRAINT fk_book_prices_format
        FOREIGN KEY (book_format_id)
            REFERENCES catalogue.book_formats (book_format_id),
    CONSTRAINT fk_book_prices_store
        FOREIGN KEY (store_id) REFERENCES store.stores (store_id),
    CONSTRAINT chk_book_prices_amount
        CHECK (amount > 0),
    CONSTRAINT chk_book_prices_dates
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- Current active price lookup: format + store, effective_to IS NULL
CREATE INDEX idx_catalogue_book_prices_active
    ON catalogue.book_prices (book_format_id, store_id)
    WHERE effective_to IS NULL AND is_deleted = FALSE;

-- Price history lookup
CREATE INDEX idx_catalogue_book_prices_format
    ON catalogue.book_prices (book_format_id, store_id, effective_from DESC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- Cross-schema: identity.author_follows
-- (Must come after catalogue.authors is created)
-- ===========================================================================
CREATE TABLE identity.author_follows (
    follow_id   UUID    NOT NULL DEFAULT gen_random_uuid(),
    member_id   UUID    NOT NULL,
    author_id   UUID    NOT NULL,
    followed_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,   -- is_deleted=TRUE means unfollowed

    CONSTRAINT pk_author_follows PRIMARY KEY (follow_id),
    CONSTRAINT fk_author_follows_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT fk_author_follows_author
        FOREIGN KEY (author_id) REFERENCES catalogue.authors (author_id)
);

CREATE UNIQUE INDEX uq_author_follows_member_author
    ON identity.author_follows (member_id, author_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_identity_follows_member
    ON identity.author_follows (member_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_identity_follows_author
    ON identity.author_follows (author_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- discovery.recommended_books
-- (Depends on: discovery.recommendation_profiles, catalogue.books)
-- ===========================================================================
CREATE TABLE discovery.recommended_books (
    entry_id    UUID            NOT NULL DEFAULT gen_random_uuid(),
    profile_id  UUID            NOT NULL,
    book_id     UUID            NOT NULL,
    score       NUMERIC(6,4)    NOT NULL,
    reason      VARCHAR(30)     NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_recommended_books PRIMARY KEY (entry_id),
    CONSTRAINT fk_rec_books_profile
        FOREIGN KEY (profile_id)
            REFERENCES discovery.recommendation_profiles (profile_id),
    CONSTRAINT fk_rec_books_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT chk_rec_books_reason
        CHECK (reason IN ('ORDER_HISTORY','CATEGORY_AFFINITY','AUTHOR_FOLLOW')),
    CONSTRAINT chk_rec_books_score
        CHECK (score >= 0 AND score <= 1)
);

CREATE UNIQUE INDEX uq_rec_books_profile_book
    ON discovery.recommended_books (profile_id, book_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_disc_rec_profile
    ON discovery.recommended_books (profile_id, score DESC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- discovery.featured_entries
-- (Depends on: discovery.featured_lists, catalogue.books)
-- ===========================================================================
CREATE TABLE discovery.featured_entries (
    entry_id    UUID    NOT NULL DEFAULT gen_random_uuid(),
    list_id     UUID    NOT NULL,
    book_id     UUID    NOT NULL,
    rank        INTEGER NOT NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_featured_entries PRIMARY KEY (entry_id),
    CONSTRAINT fk_featured_entries_list
        FOREIGN KEY (list_id) REFERENCES discovery.featured_lists (list_id),
    CONSTRAINT fk_featured_entries_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT chk_featured_entries_rank
        CHECK (rank >= 1)
);

CREATE UNIQUE INDEX uq_fe_list_book
    ON discovery.featured_entries (list_id, book_id)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_fe_list_rank
    ON discovery.featured_entries (list_id, rank)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_disc_fe_list_rank
    ON discovery.featured_entries (list_id, rank ASC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- cart.cart_items
-- (Depends on: cart.carts, catalogue.books, catalogue.book_formats)
-- ===========================================================================
CREATE TABLE cart.cart_items (
    cart_item_id    UUID            NOT NULL DEFAULT gen_random_uuid(),
    cart_id         UUID            NOT NULL,
    book_id         UUID            NOT NULL,
    book_format_id  UUID            NOT NULL,
    quantity        INTEGER         NOT NULL DEFAULT 1,
    unit_price      NUMERIC(14,2)   NOT NULL,
    currency        CHAR(3)         NOT NULL DEFAULT 'INR',
    added_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,   -- is_deleted=TRUE means item removed

    CONSTRAINT pk_cart_items PRIMARY KEY (cart_item_id),
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES cart.carts (cart_id),
    CONSTRAINT fk_cart_items_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT fk_cart_items_format
        FOREIGN KEY (book_format_id)
            REFERENCES catalogue.book_formats (book_format_id),
    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity >= 1),
    CONSTRAINT chk_cart_items_price
        CHECK (unit_price > 0)
);

CREATE UNIQUE INDEX uq_cart_items_cart_format
    ON cart.cart_items (cart_id, book_format_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_cart_items_cart
    ON cart.cart_items (cart_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_cart_items_book
    ON cart.cart_items (book_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- cart.wishlist_items
-- (Depends on: cart.wishlists, catalogue.books, catalogue.book_formats)
-- ===========================================================================
CREATE TABLE cart.wishlist_items (
    wishlist_item_id    UUID    NOT NULL DEFAULT gen_random_uuid(),
    wishlist_id         UUID    NOT NULL,
    book_id             UUID    NOT NULL,
    book_format_id      UUID    NOT NULL,
    added_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_wishlist_items PRIMARY KEY (wishlist_item_id),
    CONSTRAINT fk_wishlist_items_wishlist
        FOREIGN KEY (wishlist_id) REFERENCES cart.wishlists (wishlist_id),
    CONSTRAINT fk_wishlist_items_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT fk_wishlist_items_format
        FOREIGN KEY (book_format_id)
            REFERENCES catalogue.book_formats (book_format_id)
);

CREATE UNIQUE INDEX uq_wishlist_items_list_format
    ON cart.wishlist_items (wishlist_id, book_format_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_cart_wishlist_items_wishlist
    ON cart.wishlist_items (wishlist_id)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- Apply updated_at trigger to catalogue schema tables
-- ===========================================================================
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'catalogue'
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
