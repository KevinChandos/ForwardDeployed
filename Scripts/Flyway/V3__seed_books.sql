-- =============================================================================
-- V3__seed_books.sql
-- Book Worm — Reference data: catalogue.books, catalogue.book_formats,
--             catalogue.book_authors, catalogue.book_categories
--
-- WHY: Books depend on authors (V4) and publishers (V5), but to keep FK
--      ordering self-consistent within this single migration, all four
--      catalogue tables are populated together in dependency order:
--        publishers → authors → books → book_formats → book_authors
--                                                     → book_categories
--
--      Fixed UUID literals are used throughout so downstream seed migrations
--      and integration tests can reference rows by known IDs without lookups.
--
--      Prices and store associations are intentionally excluded here; they
--      are store-context data and should be seeded alongside store rows in
--      a future store-seed migration.
--
-- NOTE: V4__seed_authors.sql and V5__seed_publishers.sql add supplementary
--       authors and publishers beyond what this file seeds.  ON CONFLICT
--       DO NOTHING guards prevent duplicates if ordering changes.
-- =============================================================================

-- =============================================================================
-- A. Publishers (required before books)
-- =============================================================================
INSERT INTO catalogue.publishers
    (publisher_id, name, website, is_active)
VALUES
    ('00000000-0000-0000-0001-000000000001', 'Penguin Random House', 'https://www.penguinrandomhouse.com', TRUE),
    ('00000000-0000-0000-0001-000000000002', 'HarperCollins',        'https://www.harpercollins.com',      TRUE),
    ('00000000-0000-0000-0001-000000000003', 'Simon & Schuster',     'https://www.simonandschuster.com',   TRUE),
    ('00000000-0000-0000-0001-000000000004', 'Macmillan Publishers', 'https://us.macmillan.com',           TRUE),
    ('00000000-0000-0000-0001-000000000005', 'Hachette Livre',       'https://www.hachette.com',           TRUE),
    ('00000000-0000-0000-0001-000000000006', 'Oxford University Press','https://global.oup.com',           TRUE),
    ('00000000-0000-0000-0001-000000000007', 'Cambridge University Press','https://www.cambridge.org',     TRUE),
    ('00000000-0000-0000-0001-000000000008', 'Bloomsbury Publishing','https://www.bloomsbury.com',         TRUE),
    ('00000000-0000-0000-0001-000000000009', 'Scholastic',           'https://www.scholastic.com',         TRUE),
    ('00000000-0000-0000-0001-000000000010', 'O''Reilly Media',      'https://www.oreilly.com',            TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- B. Authors (required before book_authors)
-- =============================================================================
INSERT INTO catalogue.authors
    (author_id, name, bio, is_active)
VALUES
    ('00000000-0000-0000-0002-000000000001', 'George Orwell',
     'Eric Arthur Blair, known by his pen name George Orwell, was an English novelist, essayist, journalist and critic.',
     TRUE),
    ('00000000-0000-0000-0002-000000000002', 'J.K. Rowling',
     'Joanne Rowling, writing under the pen name J.K. Rowling, is a British author and philanthropist best known for the Harry Potter series.',
     TRUE),
    ('00000000-0000-0000-0002-000000000003', 'Yuval Noah Harari',
     'Israeli public intellectual, historian, and professor at the Hebrew University of Jerusalem.',
     TRUE),
    ('00000000-0000-0000-0002-000000000004', 'Chimamanda Ngozi Adichie',
     'Nigerian author whose works range from novels to short stories to nonfiction.',
     TRUE),
    ('00000000-0000-0000-0002-000000000005', 'Frank Herbert',
     'American science fiction author best known for the novel Dune and its five sequels.',
     TRUE),
    ('00000000-0000-0000-0002-000000000006', 'Toni Morrison',
     'American novelist, essayist, editor, teacher, and professor emeritus at Princeton University.',
     TRUE),
    ('00000000-0000-0000-0002-000000000007', 'Haruki Murakami',
     'Japanese writer whose books and stories have been bestsellers in Japan and internationally.',
     TRUE),
    ('00000000-0000-0000-0002-000000000008', 'Gabriel García Márquez',
     'Colombian novelist, short-story writer, screenwriter and journalist, Nobel Prize in Literature 1982.',
     TRUE),
    ('00000000-0000-0000-0002-000000000009', 'Agatha Christie',
     'English writer known for her sixty-six detective novels and fourteen short story collections.',
     TRUE),
    ('00000000-0000-0000-0002-000000000010', 'Malcolm Gladwell',
     'Canadian journalist, author and public speaker who has been a staff writer for The New Yorker since 1996.',
     TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- C. Books
-- =============================================================================
INSERT INTO catalogue.books
    (book_id, title, synopsis, language, published_date, sales_count,
     average_rating, review_count, publisher_id, is_active)
VALUES
    -- Fiction
    ('00000000-0000-0000-0003-000000000001',
     'Nineteen Eighty-Four',
     'A dystopian social science fiction novel and cautionary tale about the dangers of totalitarianism.',
     'English', '1949-06-08', 1200000, 4.75, 9800,
     '00000000-0000-0000-0001-000000000002', TRUE),

    ('00000000-0000-0000-0003-000000000002',
     'Animal Farm',
     'An allegorical novella reflecting events leading up to the Russian Revolution of 1917.',
     'English', '1945-08-17', 980000, 4.60, 7600,
     '00000000-0000-0000-0001-000000000002', TRUE),

    ('00000000-0000-0000-0003-000000000003',
     'Harry Potter and the Philosopher''s Stone',
     'A young boy discovers he is a wizard and embarks on a journey at Hogwarts School of Witchcraft and Wizardry.',
     'English', '1997-06-26', 5000000, 4.90, 42000,
     '00000000-0000-0000-0001-000000000008', TRUE),

    ('00000000-0000-0000-0003-000000000004',
     'Harry Potter and the Chamber of Secrets',
     'Harry Potter''s second year at Hogwarts brings a mysterious monster lurking within the school walls.',
     'English', '1998-07-02', 4500000, 4.85, 38000,
     '00000000-0000-0000-0001-000000000008', TRUE),

    ('00000000-0000-0000-0003-000000000005',
     'Dune',
     'Set in the distant future, the story follows young Paul Atreides as he navigates a feudal interstellar society.',
     'English', '1965-08-01', 2000000, 4.80, 17500,
     '00000000-0000-0000-0001-000000000001', TRUE),

    ('00000000-0000-0000-0003-000000000006',
     'Beloved',
     'A story about a formerly enslaved woman haunted by the ghost of her deceased daughter.',
     'English', '1987-09-16', 900000, 4.55, 6200,
     '00000000-0000-0000-0001-000000000001', TRUE),

    ('00000000-0000-0000-0003-000000000007',
     'Norwegian Wood',
     'A nostalgic story of loss and sexuality, set against the backdrop of the late 1960s in Tokyo.',
     'English', '1987-09-04', 1500000, 4.40, 11200,
     '00000000-0000-0000-0001-000000000001', TRUE),

    ('00000000-0000-0000-0003-000000000008',
     'One Hundred Years of Solitude',
     'The story of the Buendía family over seven generations in the fictional town of Macondo.',
     'English', '1967-05-30', 1800000, 4.70, 14000,
     '00000000-0000-0000-0001-000000000001', TRUE),

    ('00000000-0000-0000-0003-000000000009',
     'Murder on the Orient Express',
     'Detective Hercule Poirot investigates the murder of an American businessman on a luxury train.',
     'English', '1934-01-01', 3200000, 4.65, 25000,
     '00000000-0000-0000-0001-000000000002', TRUE),

    ('00000000-0000-0000-0003-000000000010',
     'Purple Hibiscus',
     'A story of a young Nigerian girl caught between her oppressive father and her liberating aunt.',
     'English', '2003-10-01', 600000, 4.35, 4100,
     '00000000-0000-0000-0001-000000000001', TRUE),

    -- Non-Fiction / Self-Help
    ('00000000-0000-0000-0003-000000000011',
     'Sapiens: A Brief History of Humankind',
     'Explores the ways in which biology and history have defined us and enhanced our understanding of what it means to be human.',
     'English', '2011-01-01', 3500000, 4.70, 28000,
     '00000000-0000-0000-0001-000000000002', TRUE),

    ('00000000-0000-0000-0003-000000000012',
     'Homo Deus: A Brief History of Tomorrow',
     'Explores what might happen to the world when old myths are coupled with new godlike technologies.',
     'English', '2015-09-04', 1800000, 4.40, 13000,
     '00000000-0000-0000-0001-000000000002', TRUE),

    ('00000000-0000-0000-0003-000000000013',
     'Outliers: The Story of Success',
     'Examines the factors that contribute to high levels of success, arguing that opportunity and culture matter as much as talent.',
     'English', '2008-11-18', 2200000, 4.55, 18700,
     '00000000-0000-0000-0001-000000000004', TRUE),

    ('00000000-0000-0000-0003-000000000014',
     'The Tipping Point',
     'Explores how little things can make a big difference, focusing on moments when ideas, trends, and social behaviours cross a threshold.',
     'English', '2000-03-01', 1900000, 4.45, 15500,
     '00000000-0000-0000-0001-000000000004', TRUE),

    -- Science & Tech
    ('00000000-0000-0000-0003-000000000015',
     'A Brief History of Time',
     'A landmark volume in science writing about the origin and fate of the universe.',
     'English', '1988-04-01', 2500000, 4.60, 20100,
     '00000000-0000-0000-0001-000000000001', TRUE),

    ('00000000-0000-0000-0003-000000000016',
     'The Pragmatic Programmer',
     'A guide to software craftsmanship, offering practical advice on how to program effectively.',
     'English', '1999-10-30', 700000, 4.80, 5900,
     '00000000-0000-0000-0001-000000000010', TRUE),

    ('00000000-0000-0000-0003-000000000017',
     'Clean Code: A Handbook of Agile Software Craftsmanship',
     'Presents the best practices of writing clean, readable, and maintainable code.',
     'English', '2008-08-11', 800000, 4.65, 6700,
     '00000000-0000-0000-0001-000000000002', TRUE),

    -- Business
    ('00000000-0000-0000-0003-000000000018',
     'Zero to One: Notes on Startups, or How to Build the Future',
     'Offers contrarian views on business, technology, and the future from the co-founder of PayPal.',
     'English', '2014-09-16', 1200000, 4.50, 9800,
     '00000000-0000-0000-0001-000000000003', TRUE),

    ('00000000-0000-0000-0003-000000000019',
     'The Lean Startup',
     'Introduces the lean startup methodology — a scientific approach to creating and managing successful startups.',
     'English', '2011-09-13', 1100000, 4.45, 9200,
     '00000000-0000-0000-0001-000000000003', TRUE),

    -- Young Adult
    ('00000000-0000-0000-0003-000000000020',
     'The Alchemist',
     'A philosophical novel about a young Andalusian shepherd who yearns to travel the world in search of a treasure.',
     'English', '1988-01-01', 6500000, 4.50, 51000,
     '00000000-0000-0000-0001-000000000002', TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- D. Book Formats
-- Each book gets at least a PAPERBACK format; popular titles also get EBOOK.
-- =============================================================================
INSERT INTO catalogue.book_formats
    (book_format_id, book_id, format_type, isbn, page_count, is_active)
VALUES
    -- Nineteen Eighty-Four
    ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0003-000000000001', 'PAPERBACK',  '9780451524935', 328,  TRUE),
    ('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0003-000000000001', 'EBOOK',      NULL,            328,  TRUE),

    -- Animal Farm
    ('00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0003-000000000002', 'PAPERBACK',  '9780451526342', 112,  TRUE),

    -- Harry Potter and the Philosopher's Stone
    ('00000000-0000-0000-0004-000000000004', '00000000-0000-0000-0003-000000000003', 'PAPERBACK',  '9781408855652', 352,  TRUE),
    ('00000000-0000-0000-0004-000000000005', '00000000-0000-0000-0003-000000000003', 'HARDCOVER',  '9781408845646', 352,  TRUE),
    ('00000000-0000-0000-0004-000000000006', '00000000-0000-0000-0003-000000000003', 'EBOOK',      NULL,            352,  TRUE),

    -- Harry Potter and the Chamber of Secrets
    ('00000000-0000-0000-0004-000000000007', '00000000-0000-0000-0003-000000000004', 'PAPERBACK',  '9781408855669', 368,  TRUE),
    ('00000000-0000-0000-0004-000000000008', '00000000-0000-0000-0003-000000000004', 'HARDCOVER',  '9781408845653', 368,  TRUE),

    -- Dune
    ('00000000-0000-0000-0004-000000000009', '00000000-0000-0000-0003-000000000005', 'PAPERBACK',  '9780441013593', 896,  TRUE),
    ('00000000-0000-0000-0004-000000000010', '00000000-0000-0000-0003-000000000005', 'EBOOK',      NULL,            896,  TRUE),

    -- Beloved
    ('00000000-0000-0000-0004-000000000011', '00000000-0000-0000-0003-000000000006', 'PAPERBACK',  '9781400033416', 321,  TRUE),

    -- Norwegian Wood
    ('00000000-0000-0000-0004-000000000012', '00000000-0000-0000-0003-000000000007', 'PAPERBACK',  '9780375704024', 296,  TRUE),
    ('00000000-0000-0000-0004-000000000013', '00000000-0000-0000-0003-000000000007', 'EBOOK',      NULL,            296,  TRUE),

    -- One Hundred Years of Solitude
    ('00000000-0000-0000-0004-000000000014', '00000000-0000-0000-0003-000000000008', 'PAPERBACK',  '9780060883287', 417,  TRUE),

    -- Murder on the Orient Express
    ('00000000-0000-0000-0004-000000000015', '00000000-0000-0000-0003-000000000009', 'PAPERBACK',  '9780062693662', 256,  TRUE),
    ('00000000-0000-0000-0004-000000000016', '00000000-0000-0000-0003-000000000009', 'EBOOK',      NULL,            256,  TRUE),

    -- Purple Hibiscus
    ('00000000-0000-0000-0004-000000000017', '00000000-0000-0000-0003-000000000010', 'PAPERBACK',  '9781616953638', 320,  TRUE),

    -- Sapiens
    ('00000000-0000-0000-0004-000000000018', '00000000-0000-0000-0003-000000000011', 'PAPERBACK',  '9780062316110', 443,  TRUE),
    ('00000000-0000-0000-0004-000000000019', '00000000-0000-0000-0003-000000000011', 'HARDCOVER',  '9780062316097', 443,  TRUE),
    ('00000000-0000-0000-0004-000000000020', '00000000-0000-0000-0003-000000000011', 'EBOOK',      NULL,            443,  TRUE),

    -- Homo Deus
    ('00000000-0000-0000-0004-000000000021', '00000000-0000-0000-0003-000000000012', 'PAPERBACK',  '9780062464347', 464,  TRUE),
    ('00000000-0000-0000-0004-000000000022', '00000000-0000-0000-0003-000000000012', 'EBOOK',      NULL,            464,  TRUE),

    -- Outliers
    ('00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0003-000000000013', 'PAPERBACK',  '9780316017930', 336,  TRUE),
    ('00000000-0000-0000-0004-000000000024', '00000000-0000-0000-0003-000000000013', 'EBOOK',      NULL,            336,  TRUE),

    -- The Tipping Point
    ('00000000-0000-0000-0004-000000000025', '00000000-0000-0000-0003-000000000014', 'PAPERBACK',  '9780316346627', 301,  TRUE),

    -- A Brief History of Time
    ('00000000-0000-0000-0004-000000000026', '00000000-0000-0000-0003-000000000015', 'PAPERBACK',  '9780553380163', 212,  TRUE),
    ('00000000-0000-0000-0004-000000000027', '00000000-0000-0000-0003-000000000015', 'EBOOK',      NULL,            212,  TRUE),

    -- The Pragmatic Programmer
    ('00000000-0000-0000-0004-000000000028', '00000000-0000-0000-0003-000000000016', 'PAPERBACK',  '9780201616224', 352,  TRUE),
    ('00000000-0000-0000-0004-000000000029', '00000000-0000-0000-0003-000000000016', 'EBOOK',      NULL,            352,  TRUE),

    -- Clean Code
    ('00000000-0000-0000-0004-000000000030', '00000000-0000-0000-0003-000000000017', 'PAPERBACK',  '9780132350884', 464,  TRUE),
    ('00000000-0000-0000-0004-000000000031', '00000000-0000-0000-0003-000000000017', 'EBOOK',      NULL,            464,  TRUE),

    -- Zero to One
    ('00000000-0000-0000-0004-000000000032', '00000000-0000-0000-0003-000000000018', 'PAPERBACK',  '9780804139021', 224,  TRUE),
    ('00000000-0000-0000-0004-000000000033', '00000000-0000-0000-0003-000000000018', 'EBOOK',      NULL,            224,  TRUE),

    -- The Lean Startup
    ('00000000-0000-0000-0004-000000000034', '00000000-0000-0000-0003-000000000019', 'PAPERBACK',  '9780307887894', 299,  TRUE),

    -- The Alchemist
    ('00000000-0000-0000-0004-000000000035', '00000000-0000-0000-0003-000000000020', 'PAPERBACK',  '9780062315007', 208,  TRUE),
    ('00000000-0000-0000-0004-000000000036', '00000000-0000-0000-0003-000000000020', 'HARDCOVER',  '9780062390622', 208,  TRUE),
    ('00000000-0000-0000-0004-000000000037', '00000000-0000-0000-0003-000000000020', 'EBOOK',      NULL,            208,  TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- E. Book–Author associations
-- =============================================================================
INSERT INTO catalogue.book_authors
    (book_author_id, book_id, author_id, role)
VALUES
    -- George Orwell
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000001', 'AUTHOR'),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000001', 'AUTHOR'),
    -- J.K. Rowling
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0002-000000000002', 'AUTHOR'),
    ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0002-000000000002', 'AUTHOR'),
    -- Frank Herbert
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0002-000000000005', 'AUTHOR'),
    -- Toni Morrison
    ('00000000-0000-0000-0005-000000000006', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0002-000000000006', 'AUTHOR'),
    -- Haruki Murakami
    ('00000000-0000-0000-0005-000000000007', '00000000-0000-0000-0003-000000000007', '00000000-0000-0000-0002-000000000007', 'AUTHOR'),
    -- Gabriel García Márquez
    ('00000000-0000-0000-0005-000000000008', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0002-000000000008', 'AUTHOR'),
    -- Agatha Christie
    ('00000000-0000-0000-0005-000000000009', '00000000-0000-0000-0003-000000000009', '00000000-0000-0000-0002-000000000009', 'AUTHOR'),
    -- Chimamanda Ngozi Adichie
    ('00000000-0000-0000-0005-000000000010', '00000000-0000-0000-0003-000000000010', '00000000-0000-0000-0002-000000000004', 'AUTHOR'),
    -- Yuval Noah Harari
    ('00000000-0000-0000-0005-000000000011', '00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0002-000000000003', 'AUTHOR'),
    ('00000000-0000-0000-0005-000000000012', '00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0002-000000000003', 'AUTHOR'),
    -- Malcolm Gladwell
    ('00000000-0000-0000-0005-000000000013', '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0002-000000000010', 'AUTHOR'),
    ('00000000-0000-0000-0005-000000000014', '00000000-0000-0000-0003-000000000014', '00000000-0000-0000-0002-000000000010', 'AUTHOR'),
    -- A Brief History of Time — no author row in this seed; seeded in V4
    -- The Pragmatic Programmer — authors seeded in V4
    -- Clean Code — author seeded in V4
    -- Zero to One — author seeded in V4
    -- The Lean Startup — author seeded in V4
    -- The Alchemist — author seeded in V4
ON CONFLICT DO NOTHING;

-- =============================================================================
-- F. Book–Category associations
-- Category UUIDs reference V2__seed_categories.sql fixed literals.
-- =============================================================================
INSERT INTO catalogue.book_categories
    (book_category_id, book_id, category_id)
VALUES
    -- Nineteen Eighty-Four → Literary Fiction, Science Fiction
    ('00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000201'),
    ('00000000-0000-0000-0006-000000000002', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000203'),

    -- Animal Farm → Literary Fiction
    ('00000000-0000-0000-0006-000000000003', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000201'),

    -- Harry Potter 1 → Fantasy, Young Adult
    ('00000000-0000-0000-0006-000000000004', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000204'),
    ('00000000-0000-0000-0006-000000000005', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000233'),

    -- Harry Potter 2 → Fantasy, Young Adult
    ('00000000-0000-0000-0006-000000000006', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000204'),
    ('00000000-0000-0000-0006-000000000007', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000233'),

    -- Dune → Science Fiction
    ('00000000-0000-0000-0006-000000000008', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0000-000000000203'),

    -- Beloved → Literary Fiction
    ('00000000-0000-0000-0006-000000000009', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0000-000000000201'),

    -- Norwegian Wood → Literary Fiction
    ('00000000-0000-0000-0006-000000000010', '00000000-0000-0000-0003-000000000007', '00000000-0000-0000-0000-000000000201'),

    -- One Hundred Years of Solitude → Literary Fiction
    ('00000000-0000-0000-0006-000000000011', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0000-000000000201'),

    -- Murder on the Orient Express → Mystery & Thriller
    ('00000000-0000-0000-0006-000000000012', '00000000-0000-0000-0003-000000000009', '00000000-0000-0000-0000-000000000202'),

    -- Purple Hibiscus → Literary Fiction
    ('00000000-0000-0000-0006-000000000013', '00000000-0000-0000-0003-000000000010', '00000000-0000-0000-0000-000000000201'),

    -- Sapiens → Non-Fiction, World History
    ('00000000-0000-0000-0006-000000000014', '00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0000-000000000102'),
    ('00000000-0000-0000-0006-000000000015', '00000000-0000-0000-0003-000000000011', '00000000-0000-0000-0000-000000000261'),

    -- Homo Deus → Non-Fiction, Political Science
    ('00000000-0000-0000-0006-000000000016', '00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0000-000000000102'),
    ('00000000-0000-0000-0006-000000000017', '00000000-0000-0000-0003-000000000012', '00000000-0000-0000-0000-000000000262'),

    -- Outliers → Self-Help, Business & Finance
    ('00000000-0000-0000-0006-000000000018', '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000105'),
    ('00000000-0000-0000-0006-000000000019', '00000000-0000-0000-0003-000000000013', '00000000-0000-0000-0000-000000000107'),

    -- The Tipping Point → Self-Help
    ('00000000-0000-0000-0006-000000000020', '00000000-0000-0000-0003-000000000014', '00000000-0000-0000-0000-000000000105'),

    -- A Brief History of Time → Physics
    ('00000000-0000-0000-0006-000000000021', '00000000-0000-0000-0003-000000000015', '00000000-0000-0000-0000-000000000223'),

    -- The Pragmatic Programmer → Computer Science
    ('00000000-0000-0000-0006-000000000022', '00000000-0000-0000-0003-000000000016', '00000000-0000-0000-0000-000000000221'),

    -- Clean Code → Computer Science
    ('00000000-0000-0000-0006-000000000023', '00000000-0000-0000-0003-000000000017', '00000000-0000-0000-0000-000000000221'),

    -- Zero to One → Entrepreneurship, Business & Finance
    ('00000000-0000-0000-0006-000000000024', '00000000-0000-0000-0003-000000000018', '00000000-0000-0000-0000-000000000251'),
    ('00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0003-000000000018', '00000000-0000-0000-0000-000000000107'),

    -- The Lean Startup → Entrepreneurship
    ('00000000-0000-0000-0006-000000000026', '00000000-0000-0000-0003-000000000019', '00000000-0000-0000-0000-000000000251'),

    -- The Alchemist → Young Adult, Self-Help
    ('00000000-0000-0000-0006-000000000027', '00000000-0000-0000-0003-000000000020', '00000000-0000-0000-0000-000000000233'),
    ('00000000-0000-0000-0006-000000000028', '00000000-0000-0000-0003-000000000020', '00000000-0000-0000-0000-000000000105')
ON CONFLICT DO NOTHING;
