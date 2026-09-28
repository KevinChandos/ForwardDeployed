-- =============================================================================
-- V4__seed_authors.sql
-- Book Worm — Extended reference data: catalogue.authors + book_authors links
--
-- WHY: V3 seeded the 10 authors directly associated with the initial 20 books.
--      This migration adds a broader author roster used by books that were
--      referenced in V3 but whose author rows were deferred (The Pragmatic
--      Programmer, Clean Code, Zero to One, The Lean Startup, The Alchemist,
--      A Brief History of Time), plus standalone authors for future catalogue
--      expansion.  The book_authors rows that were intentionally left out of
--      V3 are inserted here, completing all FK relationships.
--
--      ON CONFLICT DO NOTHING guards prevent re-insert failures if migrations
--      are replayed (e.g. in test environments with a clean database state).
-- =============================================================================

-- =============================================================================
-- A. Additional authors
-- =============================================================================
INSERT INTO catalogue.authors
    (author_id, name, bio, is_active)
VALUES
    -- Authors for deferred book_authors rows from V3
    ('00000000-0000-0000-0002-000000000011', 'Stephen Hawking',
     'English theoretical physicist, cosmologist, and author who was director of research at the Centre for Theoretical Cosmology at the University of Cambridge.',
     TRUE),

    ('00000000-0000-0000-0002-000000000012', 'Andrew Hunt',
     'Co-author of The Pragmatic Programmer; co-founder of the Agile Alliance and one of the 17 original signatories of the Agile Manifesto.',
     TRUE),

    ('00000000-0000-0000-0002-000000000013', 'David Thomas',
     'Co-author of The Pragmatic Programmer; software developer and consultant.',
     TRUE),

    ('00000000-0000-0000-0002-000000000014', 'Robert C. Martin',
     'Also known as Uncle Bob, American software engineer, instructor, and author. Author of Clean Code and The Clean Coder.',
     TRUE),

    ('00000000-0000-0000-0002-000000000015', 'Peter Thiel',
     'American entrepreneur, venture capitalist, and political activist. Co-founder of PayPal and Palantir Technologies.',
     TRUE),

    ('00000000-0000-0000-0002-000000000016', 'Blake Masters',
     'American entrepreneur and co-author of Zero to One with Peter Thiel.',
     TRUE),

    ('00000000-0000-0000-0002-000000000017', 'Eric Ries',
     'Entrepreneur and author of The Lean Startup, a methodology for developing businesses and products.',
     TRUE),

    ('00000000-0000-0000-0002-000000000018', 'Paulo Coelho',
     'Brazilian lyricist and novelist. Best known for his novel The Alchemist.',
     TRUE),

    -- Additional standalone authors for catalogue growth
    ('00000000-0000-0000-0002-000000000019', 'Fyodor Dostoevsky',
     'Russian novelist, short story writer, essayist, journalist and philosopher. Works include Crime and Punishment and The Brothers Karamazov.',
     TRUE),

    ('00000000-0000-0000-0002-000000000020', 'Leo Tolstoy',
     'Russian writer who is regarded as one of the greatest authors of all time. Works include War and Peace and Anna Karenina.',
     TRUE),

    ('00000000-0000-0000-0002-000000000021', 'Jane Austen',
     'English novelist known primarily for her six major novels — including Sense and Sensibility, Pride and Prejudice, and Emma.',
     TRUE),

    ('00000000-0000-0000-0002-000000000022', 'Charles Dickens',
     'English writer and social critic who created some of the world''s best-known fictional characters.',
     TRUE),

    ('00000000-0000-0000-0002-000000000023', 'Virginia Woolf',
     'English writer considered one of the most important modernist 20th-century authors.',
     TRUE),

    ('00000000-0000-0000-0002-000000000024', 'Ernest Hemingway',
     'American novelist, short-story writer, and journalist. Known for works such as A Farewell to Arms and The Old Man and the Sea.',
     TRUE),

    ('00000000-0000-0000-0002-000000000025', 'Franz Kafka',
     'German-speaking Bohemian novelist and short-story writer. Works include The Metamorphosis and The Trial.',
     TRUE),

    ('00000000-0000-0000-0002-000000000026', 'Isabel Allende',
     'Chilean American author who writes novels, memoirs, and short stories. Best known for The House of the Spirits.',
     TRUE),

    ('00000000-0000-0000-0002-000000000027', 'Salman Rushdie',
     'British-Indian novelist and essayist. Known for his work exploring historical fiction and magical realism.',
     TRUE),

    ('00000000-0000-0000-0002-000000000028', 'Kazuo Ishiguro',
     'British novelist, screenwriter, musician, and short-story writer. Nobel Prize in Literature 2017.',
     TRUE),

    ('00000000-0000-0000-0002-000000000029', 'Arundhati Roy',
     'Indian author, activist, and public intellectual. Author of The God of Small Things, winner of the Booker Prize.',
     TRUE),

    ('00000000-0000-0000-0002-000000000030', 'Khaled Hosseini',
     'Afghan-American novelist and physician. Known for The Kite Runner and A Thousand Splendid Suns.',
     TRUE),

    ('00000000-0000-0000-0002-000000000031', 'Stieg Larsson',
     'Swedish journalist and writer known for The Girl with the Dragon Tattoo trilogy.',
     FALSE),   -- deceased; is_active = FALSE to indicate no new works

    ('00000000-0000-0000-0002-000000000032', 'Dan Brown',
     'American author best known for his thriller novels, including The Da Vinci Code.',
     TRUE),

    ('00000000-0000-0000-0002-000000000033', 'Gillian Flynn',
     'American novelist and former television critic. Author of Gone Girl, Sharp Objects, and Dark Places.',
     TRUE),

    ('00000000-0000-0000-0002-000000000034', 'Neil Gaiman',
     'English author of short fiction, novels, comic books, graphic novels, nonfiction, audio theatre, and films.',
     TRUE),

    ('00000000-0000-0000-0002-000000000035', 'Terry Pratchett',
     'English humorist, satirist, and author of fantasy novels, especially those in the Discworld series.',
     FALSE),   -- deceased; is_active = FALSE

    ('00000000-0000-0000-0002-000000000036', 'Brandon Sanderson',
     'American author of epic fantasy and science fiction. Known for the Stormlight Archive and Mistborn series.',
     TRUE),

    ('00000000-0000-0000-0002-000000000037', 'Patrick Rothfuss',
     'American author of epic fantasy. Author of The Name of the Wind.',
     TRUE),

    ('00000000-0000-0000-0002-000000000038', 'Robin Sharma',
     'Canadian writer, leadership expert, and motivational speaker. Author of The Monk Who Sold His Ferrari.',
     TRUE),

    ('00000000-0000-0000-0002-000000000039', 'James Clear',
     'Author, entrepreneur, and photographer. Author of Atomic Habits.',
     TRUE),

    ('00000000-0000-0000-0002-000000000040', 'Daniel Kahneman',
     'Israeli-American psychologist and economist notable for his work on the psychology of judgment and decision-making. Nobel Memorial Prize in Economic Sciences 2002.',
     TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- B. book_authors links deferred from V3
--    These rows complete the author associations for books whose author UUIDs
--    were not yet inserted when V3 ran.
-- =============================================================================
INSERT INTO catalogue.book_authors
    (book_author_id, book_id, author_id, role)
VALUES
    -- A Brief History of Time → Stephen Hawking
    ('00000000-0000-0000-0005-000000000015',
     '00000000-0000-0000-0003-000000000015',
     '00000000-0000-0000-0002-000000000011',
     'AUTHOR'),

    -- The Pragmatic Programmer → Andrew Hunt (AUTHOR), David Thomas (CO_AUTHOR)
    ('00000000-0000-0000-0005-000000000016',
     '00000000-0000-0000-0003-000000000016',
     '00000000-0000-0000-0002-000000000012',
     'AUTHOR'),
    ('00000000-0000-0000-0005-000000000017',
     '00000000-0000-0000-0003-000000000016',
     '00000000-0000-0000-0002-000000000013',
     'CO_AUTHOR'),

    -- Clean Code → Robert C. Martin
    ('00000000-0000-0000-0005-000000000018',
     '00000000-0000-0000-0003-000000000017',
     '00000000-0000-0000-0002-000000000014',
     'AUTHOR'),

    -- Zero to One → Peter Thiel (AUTHOR), Blake Masters (CO_AUTHOR)
    ('00000000-0000-0000-0005-000000000019',
     '00000000-0000-0000-0003-000000000018',
     '00000000-0000-0000-0002-000000000015',
     'AUTHOR'),
    ('00000000-0000-0000-0005-000000000020',
     '00000000-0000-0000-0003-000000000018',
     '00000000-0000-0000-0002-000000000016',
     'CO_AUTHOR'),

    -- The Lean Startup → Eric Ries
    ('00000000-0000-0000-0005-000000000021',
     '00000000-0000-0000-0003-000000000019',
     '00000000-0000-0000-0002-000000000017',
     'AUTHOR'),

    -- The Alchemist → Paulo Coelho
    ('00000000-0000-0000-0005-000000000022',
     '00000000-0000-0000-0003-000000000020',
     '00000000-0000-0000-0002-000000000018',
     'AUTHOR')
ON CONFLICT DO NOTHING;
