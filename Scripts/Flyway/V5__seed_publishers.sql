-- =============================================================================
-- V5__seed_publishers.sql
-- Book Worm — Extended reference data: catalogue.publishers
--
-- WHY: V3 seeded 10 publishers that are directly referenced by the initial
--      book rows.  This migration adds the broader publisher roster covering
--      academic presses, regional publishers, and digital-first imprints that
--      the catalogue team will need when onboarding additional titles.
--
--      All rows are idempotent (ON CONFLICT DO NOTHING) so the migration is
--      safe to replay in test or staging environments.
--
--      UUID range: 00000000-0000-0000-0001-0000000000xx (continuing from V3)
-- =============================================================================

-- =============================================================================
-- A. Additional global publishers
-- =============================================================================
INSERT INTO catalogue.publishers
    (publisher_id, name, website, is_active)
VALUES
    -- Academic & Technical
    ('00000000-0000-0000-0001-000000000011', 'MIT Press',                  'https://mitpress.mit.edu',           TRUE),
    ('00000000-0000-0000-0001-000000000012', 'Princeton University Press', 'https://press.princeton.edu',        TRUE),
    ('00000000-0000-0000-0001-000000000013', 'Harvard University Press',   'https://www.hup.harvard.edu',        TRUE),
    ('00000000-0000-0000-0001-000000000014', 'Yale University Press',      'https://yalebooks.yale.edu',         TRUE),
    ('00000000-0000-0000-0001-000000000015', 'Stanford University Press',  'https://www.sup.org',                TRUE),
    ('00000000-0000-0000-0001-000000000016', 'Wiley',                      'https://www.wiley.com',              TRUE),
    ('00000000-0000-0000-0001-000000000017', 'Springer',                   'https://www.springer.com',           TRUE),
    ('00000000-0000-0000-0001-000000000018', 'Pearson Education',          'https://www.pearson.com',            TRUE),
    ('00000000-0000-0000-0001-000000000019', 'McGraw-Hill',                'https://www.mheducation.com',        TRUE),
    ('00000000-0000-0000-0001-000000000020', 'Palgrave Macmillan',         'https://www.palgrave.com',           TRUE),

    -- Literary & Independent
    ('00000000-0000-0000-0001-000000000021', 'Farrar, Straus and Giroux',  'https://us.macmillan.com/fsg',       TRUE),
    ('00000000-0000-0000-0001-000000000022', 'Knopf Doubleday',            'https://knopfdoubleday.com',         TRUE),
    ('00000000-0000-0000-0001-000000000023', 'W. W. Norton & Company',     'https://wwnorton.com',               TRUE),
    ('00000000-0000-0000-0001-000000000024', 'Vintage Books',              'https://www.penguinrandomhouse.com/imprint/vintage-books', TRUE),
    ('00000000-0000-0000-0001-000000000025', 'Anchor Books',               'https://www.penguinrandomhouse.com/imprint/anchor-books',  TRUE),
    ('00000000-0000-0000-0001-000000000026', 'Picador',                    'https://us.macmillan.com/picador',   TRUE),
    ('00000000-0000-0000-0001-000000000027', 'Canongate Books',            'https://canongate.co.uk',            TRUE),
    ('00000000-0000-0000-0001-000000000028', 'Faber & Faber',              'https://www.faber.co.uk',            TRUE),
    ('00000000-0000-0000-0001-000000000029', 'Chatto & Windus',            'https://www.penguin.co.uk/imprints/chatto-windus', TRUE),
    ('00000000-0000-0000-0001-000000000030', 'Hamish Hamilton',            'https://www.penguin.co.uk/imprints/hamish-hamilton', TRUE),

    -- Children's & YA Specialist
    ('00000000-0000-0000-0001-000000000031', 'Walker Books',               'https://www.walker.co.uk',           TRUE),
    ('00000000-0000-0000-0001-000000000032', 'Puffin Books',               'https://www.penguin.co.uk/imprints/puffin', TRUE),
    ('00000000-0000-0000-0001-000000000033', 'Candlewick Press',           'https://www.candlewick.com',         TRUE),
    ('00000000-0000-0000-0001-000000000034', 'Chronicle Books',            'https://www.chroniclebooks.com',     TRUE),
    ('00000000-0000-0000-0001-000000000035', 'Roaring Brook Press',        'https://us.macmillan.com/roaringbrook', TRUE),

    -- Business & Self-Help Specialist
    ('00000000-0000-0000-0001-000000000036', 'Portfolio Penguin',          'https://www.penguin.com/meet/publishers/portfolio/', TRUE),
    ('00000000-0000-0000-0001-000000000037', 'Currency (Crown Business)',  'https://crownpublishing.com/imprint/currency/', TRUE),
    ('00000000-0000-0000-0001-000000000038', 'Jossey-Bass',                'https://www.wiley.com/imprint/jossey-bass', TRUE),
    ('00000000-0000-0000-0001-000000000039', 'Berrett-Koehler Publishers', 'https://www.bkpublishers.com',       TRUE),
    ('00000000-0000-0000-0001-000000000040', 'Hay House',                  'https://www.hayhouse.com',           TRUE),

    -- Science & Technology Specialist
    ('00000000-0000-0000-0001-000000000041', 'Addison-Wesley',             'https://www.pearson.com/addison-wesley', TRUE),
    ('00000000-0000-0000-0001-000000000042', 'No Starch Press',            'https://nostarch.com',               TRUE),
    ('00000000-0000-0000-0001-000000000043', 'Manning Publications',       'https://www.manning.com',            TRUE),
    ('00000000-0000-0000-0001-000000000044', 'Packt Publishing',           'https://www.packtpub.com',           TRUE),
    ('00000000-0000-0000-0001-000000000045', 'Apress',                     'https://www.apress.com',             TRUE),

    -- Regional & International
    ('00000000-0000-0000-0001-000000000046', 'Penguin India',              'https://www.penguin.co.in',          TRUE),
    ('00000000-0000-0000-0001-000000000047', 'HarperCollins India',        'https://www.harpercollins.co.in',    TRUE),
    ('00000000-0000-0000-0001-000000000048', 'Westland Publications',      'https://westlandbooks.in',           TRUE),
    ('00000000-0000-0000-0001-000000000049', 'Juggernaut Books',           'https://juggernaut.in',              TRUE),
    ('00000000-0000-0000-0001-000000000050', 'Speaking Tiger',             'https://www.speakingtiger.com',      TRUE),

    -- Digital-First & Hybrid
    ('00000000-0000-0000-0001-000000000051', 'Kindle Direct Publishing',   'https://kdp.amazon.com',             TRUE),
    ('00000000-0000-0000-0001-000000000052', 'Smashwords',                 'https://www.smashwords.com',         TRUE),
    ('00000000-0000-0000-0001-000000000053', 'Inkshares',                  'https://www.inkshares.com',          TRUE),
    ('00000000-0000-0000-0001-000000000054', 'Lulu Press',                 'https://www.lulu.com',               TRUE),
    ('00000000-0000-0000-0001-000000000055', 'BookBaby',                   'https://www.bookbaby.com',           TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- B. Inactive / historical publishers (archived, merged, or renamed)
--    Stored with is_active = FALSE so existing book FK relationships remain
--    intact while the publisher no longer appears in live catalogue browsing.
-- =============================================================================
INSERT INTO catalogue.publishers
    (publisher_id, name, website, is_active)
VALUES
    ('00000000-0000-0000-0001-000000000061', 'Methuen (historical)',      NULL, FALSE),
    ('00000000-0000-0000-0001-000000000062', 'Victor Gollancz Ltd',       NULL, FALSE),
    ('00000000-0000-0000-0001-000000000063', 'Martin Secker & Warburg',   NULL, FALSE),
    ('00000000-0000-0000-0001-000000000064', 'Chapman & Hall (historical)', NULL, FALSE)
ON CONFLICT DO NOTHING;
