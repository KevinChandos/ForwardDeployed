-- =============================================================================
-- deploy/postgres/init/01_create_roles.sql
--
-- PostgreSQL initialisation script — runs exactly once on a fresh data volume.
-- Executed by the official postgres:16-alpine image from
-- /docker-entrypoint-initdb.d/ before the database is opened to connections.
--
-- WHY a separate init script instead of putting this in Flyway:
--   Flyway runs as the application role (bookworm) and therefore cannot CREATE
--   ROLE or grant superuser-level privileges.  Database-level bootstrapping
--   (role creation, extension whitelist, search_path defaults) must be done
--   by the postgres superuser before the application role exists.
--
-- IDEMPOTENCY: every statement uses IF NOT EXISTS / DO $$ blocks so the script
-- is safe to inspect or re-apply without side effects.
--
-- Target: PostgreSQL 16
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Application role
--    WHY LOGIN: the application pool authenticates as this role.
--    WHY NOSUPERUSER NOCREATEDB NOCREATEROLE: principle of least privilege —
--    the app role has no administrative capabilities.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = current_setting('POSTGRES_USER', true)) THEN
        EXECUTE format(
            'CREATE ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L',
            current_setting('POSTGRES_USER', true),
            current_setting('POSTGRES_PASSWORD', true)
        );
        RAISE NOTICE 'Created application role: %', current_setting('POSTGRES_USER', true);
    ELSE
        RAISE NOTICE 'Role already exists (skipping): %', current_setting('POSTGRES_USER', true);
    END IF;
END;
$$;

-- ---------------------------------------------------------------------------
-- 2. Grant CONNECT on the application database
--    The database was already created by POSTGRES_DB env var processing.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_db   TEXT := current_database();
    v_role TEXT := current_setting('POSTGRES_USER', true);
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I', v_db, v_role);
    RAISE NOTICE 'Granted CONNECT on % to %', v_db, v_role;
END;
$$;

-- ---------------------------------------------------------------------------
-- 3. Enable extensions required by the application
--    WHY pgcrypto: provides gen_random_uuid() used in default column values.
--    Extensions must be created by a superuser (postgres), so they belong here
--    rather than in a Flyway migration that runs as the app role.
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- 4. Set default search_path for the application role
--    WHY: Hibernate entities reference schema-qualified tables
--    (e.g. identity.members).  Setting search_path here ensures that any
--    unqualified reference falls back to public, matching the Hibernate
--    default_schema=public configuration in application.yml.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_role TEXT := current_setting('POSTGRES_USER', true);
BEGIN
    EXECUTE format(
        'ALTER ROLE %I SET search_path TO public, identity, catalogue, store, discovery, cart, ordering, promotions, checkout, payment, wallet, shipping, review, notification, outbox',
        v_role
    );
    RAISE NOTICE 'Set search_path for role: %', v_role;
END;
$$;

-- ---------------------------------------------------------------------------
-- 5. Grant CREATE on the public schema so Flyway can create the
--    flyway_schema_history table on first run.
--    WHY only public: application schemas are created by Flyway migration V1
--    using the same role; GRANT USAGE + ALTER DEFAULT PRIVILEGES below handle
--    those schemas after they exist.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_role TEXT := current_setting('POSTGRES_USER', true);
BEGIN
    EXECUTE format('GRANT CREATE, USAGE ON SCHEMA public TO %I', v_role);
    RAISE NOTICE 'Granted CREATE+USAGE on schema public to %', v_role;
END;
$$;

-- ---------------------------------------------------------------------------
-- NOTE
-- After Flyway runs V1__initial_schema.sql (which creates all bounded-context
-- schemas), the application role automatically owns those schemas because
-- Flyway connects as that role.  No further GRANT is needed for schemas
-- created by the migration.
-- ---------------------------------------------------------------------------
