-- =============================================================================
-- 006_reviews.sql
-- Schema: review
-- Covers: review.reviews
--
-- Prerequisites: 001_users.sql, 002_catalog.sql must be executed first.
--   - review.reviews references catalogue.books, identity.members
--
-- Business rules enforced:
--   - One review per member per book (partial unique index on active rows)
--   - Rating must be an integer in range 1–5
--   - Status transitions: PENDING → PUBLISHED | REJECTED
--   - After publishing, a ReviewPublished event triggers an aggregate rating
--     update on catalogue.books (handled by application event handler)
--
-- Audit columns on every table:
--   created_at, created_by, updated_at, updated_by, version, is_deleted
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS review;

-- ===========================================================================
-- review.reviews
-- ===========================================================================
CREATE TABLE review.reviews (
    review_id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    book_id             UUID        NOT NULL,
    member_id           UUID        NOT NULL,
    rating              SMALLINT    NOT NULL,
    body                TEXT        NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at        TIMESTAMPTZ NULL,
    rejection_reason    VARCHAR(500) NULL,

    -- Audit
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID        NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID        NULL,
    version     INTEGER     NOT NULL DEFAULT 1,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_reviews PRIMARY KEY (review_id),
    CONSTRAINT fk_reviews_book
        FOREIGN KEY (book_id) REFERENCES catalogue.books (book_id),
    CONSTRAINT fk_reviews_member
        FOREIGN KEY (member_id) REFERENCES identity.members (member_id),
    CONSTRAINT chk_reviews_rating
        CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_status
        CHECK (status IN ('PENDING','PUBLISHED','REJECTED'))
);

-- One active review per member per book
CREATE UNIQUE INDEX uq_reviews_book_member
    ON review.reviews (book_id, member_id)
    WHERE is_deleted = FALSE;

-- Published reviews for a book, most recent first (used on PDP)
CREATE INDEX idx_rev_reviews_book
    ON review.reviews (book_id, status, published_at DESC)
    WHERE is_deleted = FALSE;

-- All reviews by a specific member
CREATE INDEX idx_rev_reviews_member
    ON review.reviews (member_id)
    WHERE is_deleted = FALSE;

-- Moderation queue: all PENDING reviews
CREATE INDEX idx_rev_reviews_status
    ON review.reviews (status, submitted_at ASC)
    WHERE is_deleted = FALSE;

-- ===========================================================================
-- Apply updated_at trigger to review schema tables
-- ===========================================================================
DO $$
DECLARE
    t RECORD;
BEGIN
    FOR t IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'review'
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
