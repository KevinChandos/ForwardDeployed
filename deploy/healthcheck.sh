#!/bin/sh
# =============================================================================
# deploy/healthcheck.sh — Postgres container health check wrapper
#
# WHY a shell script instead of an inline CMD:
#   The postgres:16-alpine image does not have wget/curl, so the Docker
#   HEALTHCHECK must use pg_isready (included in the postgres image).
#   Wrapping it here lets us add retry logic, structured logging, and a
#   configurable timeout without bloating docker-compose.yml.
#
# Usage (invoked by Docker HEALTHCHECK or manually):
#   sh /deploy/healthcheck.sh
#
# Environment variables (all optional — defaults match docker-compose.yml):
#   POSTGRES_USER     role to authenticate as (default: bookworm)
#   POSTGRES_DB       database to check      (default: bookworm)
#   POSTGRES_HOST     hostname               (default: localhost)
#   POSTGRES_PORT     port number            (default: 5432)
# =============================================================================

set -e

PGUSER="${POSTGRES_USER:-bookworm}"
PGDATABASE="${POSTGRES_DB:-bookworm}"
PGHOST="${POSTGRES_HOST:-localhost}"
PGPORT="${POSTGRES_PORT:-5432}"

# pg_isready exits 0 when the server is accepting connections, 1 when refusing
# connections, and 2 when there is no response at all.
# WHY -q: suppress the human-readable status line so Docker log is not flooded
# on every health check poll; failures still set a non-zero exit code.
pg_isready \
    -h "${PGHOST}" \
    -p "${PGPORT}" \
    -U "${PGUSER}" \
    -d "${PGDATABASE}" \
    -q

EXIT_CODE=$?

if [ "${EXIT_CODE}" -eq 0 ]; then
    # Docker treats exit 0 as healthy.
    exit 0
elif [ "${EXIT_CODE}" -eq 1 ]; then
    # Server is running but refusing connections (starting up, recovery mode, etc.).
    # Docker treats any non-zero exit as unhealthy and will retry up to --retries times.
    echo "PostgreSQL is rejecting connections on ${PGHOST}:${PGPORT}" >&2
    exit 1
else
    # No response at all — container may still be starting.
    echo "PostgreSQL is not reachable at ${PGHOST}:${PGPORT}" >&2
    exit 2
fi
