-- =============================================================================
-- V2__seed_categories.sql
-- Book Worm — Reference data: catalogue.categories
--
-- WHY: Categories are the foundational taxonomy that books, featured lists, and
--      recommendation profiles all reference.  Seeding them in a dedicated
--      migration keeps the taxonomy stable and independent of book/author data,
--      making it safe to re-run in any environment without creating duplicates
--      (ON CONFLICT DO NOTHING).
--
-- Structure:
--   • Top-level parent categories (parent_category_id IS NULL)
--   • Level-2 sub-categories (parent_category_id set to parent UUID literal)
--
-- UUIDs are fixed literals so later migrations can reference them by FK without
-- querying back for the generated ID.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Top-level categories (parent_category_id = NULL)
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'Fiction',            'fiction',            NULL),
    ('00000000-0000-0000-0000-000000000102', 'Non-Fiction',        'non-fiction',        NULL),
    ('00000000-0000-0000-0000-000000000103', 'Science & Tech',     'science-tech',       NULL),
    ('00000000-0000-0000-0000-000000000104', 'Children''s',        'childrens',          NULL),
    ('00000000-0000-0000-0000-000000000105', 'Self-Help',          'self-help',          NULL),
    ('00000000-0000-0000-0000-000000000106', 'History & Politics', 'history-politics',   NULL),
    ('00000000-0000-0000-0000-000000000107', 'Business & Finance', 'business-finance',   NULL),
    ('00000000-0000-0000-0000-000000000108', 'Arts & Photography', 'arts-photography',   NULL),
    ('00000000-0000-0000-0000-000000000109', 'Health & Wellness',  'health-wellness',    NULL),
    ('00000000-0000-0000-0000-000000000110', 'Travel',             'travel',             NULL)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Fiction sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000201', 'Literary Fiction',    'literary-fiction',    '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000202', 'Mystery & Thriller',  'mystery-thriller',    '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000203', 'Science Fiction',     'science-fiction',     '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000204', 'Fantasy',             'fantasy',             '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000205', 'Romance',             'romance',             '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000206', 'Historical Fiction',  'historical-fiction',  '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000207', 'Horror',              'horror',              '00000000-0000-0000-0000-000000000101'),
    ('00000000-0000-0000-0000-000000000208', 'Short Stories',       'short-stories',       '00000000-0000-0000-0000-000000000101')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Non-Fiction sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000211', 'Biography & Memoir',    'biography-memoir',     '00000000-0000-0000-0000-000000000102'),
    ('00000000-0000-0000-0000-000000000212', 'Essays',                'essays',               '00000000-0000-0000-0000-000000000102'),
    ('00000000-0000-0000-0000-000000000213', 'Journalism',            'journalism',           '00000000-0000-0000-0000-000000000102'),
    ('00000000-0000-0000-0000-000000000214', 'True Crime',            'true-crime',           '00000000-0000-0000-0000-000000000102')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Science & Tech sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000221', 'Computer Science',     'computer-science',     '00000000-0000-0000-0000-000000000103'),
    ('00000000-0000-0000-0000-000000000222', 'Mathematics',          'mathematics',          '00000000-0000-0000-0000-000000000103'),
    ('00000000-0000-0000-0000-000000000223', 'Physics',              'physics',              '00000000-0000-0000-0000-000000000103'),
    ('00000000-0000-0000-0000-000000000224', 'Biology',              'biology',              '00000000-0000-0000-0000-000000000103'),
    ('00000000-0000-0000-0000-000000000225', 'Artificial Intelligence', 'artificial-intelligence', '00000000-0000-0000-0000-000000000103')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Children's sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000231', 'Picture Books',        'picture-books',        '00000000-0000-0000-0000-000000000104'),
    ('00000000-0000-0000-0000-000000000232', 'Middle Grade',         'middle-grade',         '00000000-0000-0000-0000-000000000104'),
    ('00000000-0000-0000-0000-000000000233', 'Young Adult',          'young-adult',          '00000000-0000-0000-0000-000000000104')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Self-Help sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000241', 'Productivity',         'productivity',         '00000000-0000-0000-0000-000000000105'),
    ('00000000-0000-0000-0000-000000000242', 'Mindfulness',          'mindfulness',          '00000000-0000-0000-0000-000000000105'),
    ('00000000-0000-0000-0000-000000000243', 'Personal Finance',     'personal-finance',     '00000000-0000-0000-0000-000000000105')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Business & Finance sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000251', 'Entrepreneurship',     'entrepreneurship',     '00000000-0000-0000-0000-000000000107'),
    ('00000000-0000-0000-0000-000000000252', 'Leadership',           'leadership',           '00000000-0000-0000-0000-000000000107'),
    ('00000000-0000-0000-0000-000000000253', 'Investing',            'investing',            '00000000-0000-0000-0000-000000000107'),
    ('00000000-0000-0000-0000-000000000254', 'Marketing',            'marketing',            '00000000-0000-0000-0000-000000000107')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- History & Politics sub-categories
-- ---------------------------------------------------------------------------
INSERT INTO catalogue.categories
    (category_id, name, slug, parent_category_id)
VALUES
    ('00000000-0000-0000-0000-000000000261', 'World History',        'world-history',        '00000000-0000-0000-0000-000000000106'),
    ('00000000-0000-0000-0000-000000000262', 'Political Science',    'political-science',    '00000000-0000-0000-0000-000000000106'),
    ('00000000-0000-0000-0000-000000000263', 'Philosophy',           'philosophy',           '00000000-0000-0000-0000-000000000106')
ON CONFLICT DO NOTHING;
